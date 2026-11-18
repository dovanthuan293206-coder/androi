package com.example.aistudymentor

import android.content.Intent
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.aistudymentor.data.StudyAi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File

@Composable
internal fun QuestionPanel(
    question: String,
    updateQuestion: (String) -> Unit,
    preferredLevel: String,
    style: String,
    apiKey: String,
    scroll: ScrollState,
    showAnswer: (String, String, String) -> Unit
) {
    val context = LocalContext.current

    var imageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var imageError by remember { mutableStateOf("") }
    var sentQuestion by rememberSaveable { mutableStateOf("") }
    var response by rememberSaveable { mutableStateOf("") }
    var imageRequested by rememberSaveable { mutableStateOf(false) }
    var responseAsImage by rememberSaveable { mutableStateOf(false) }
    var responseVersion by rememberSaveable { mutableIntStateOf(0) }
    var illustration by remember { mutableStateOf<Bitmap?>(null) }
    var generatingImage by remember { mutableStateOf(false) }
    var sample by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(responseVersion) {
        if (response.isNotBlank()) scroll.scrollTo(0)
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            imageUri = uri.toString()
            imageError = ""
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri = pendingPhoto
            imageError = ""
        }
        pendingPhoto = null
    }
    if (response.isNotBlank()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(sentQuestion, Modifier.background(Lilac, RoundedCornerShape(16.dp)).padding(14.dp))
        }
        key(responseVersion) {
            AnswerCard(
                response, if (sample) "Lời giải mẫu" else "AI Mentor", responseAsImage,
                illustration = illustration, generatingImage = generatingImage,
                generateImage = if (sample) null else { { StudyAi.illustrate(apiKey, response) } }
            )
        }
    }
    var attachmentsOpen by remember { mutableStateOf(false) }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.let { updateQuestion((question + " " + it).trim().take(1000)) }
        }
    }
    Surface(shape = RoundedCornerShape(28.dp), color = Color.White, tonalElevation = 2.dp) {
        Column(Modifier.padding(8.dp)) {
            if (imageUri != null) {
                QuestionImage(Uri.parse(imageUri))
                TextButton(onClick = { imageUri = null }) { Text("Bỏ ảnh") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { attachmentsOpen = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Thêm ảnh")
                    }
                    DropdownMenu(expanded = attachmentsOpen, onDismissRequest = { attachmentsOpen = false }) {
                        DropdownMenuItem(text = { Text("Tải ảnh lên") }, onClick = {
                            attachmentsOpen = false
                            picker.launch(arrayOf("image/*"))
                        })
                        DropdownMenuItem(text = { Text("Chụp ảnh") }, onClick = {
                            attachmentsOpen = false
                try {
                    val folder = File(context.cacheDir, "question-images").apply { mkdirs() }
                    val photo = File.createTempFile("question-", ".jpg", folder)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.question-images", photo)
                    pendingPhoto = uri.toString()
                    camera.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    pendingPhoto = null
                    imageError = "Thiết bị chưa có ứng dụng camera."
                } catch (_: IOException) {
                    pendingPhoto = null
                    imageError = "Không tạo được file ảnh."
                }

                        })
                    }
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = question,
                    onValueChange = { if (it.length <= 1000) updateQuestion(it) },
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                    maxLines = 5,
                    decorationBox = { inner ->
                        if (question.isEmpty()) Text("Hỏi AI Study Mentor…", color = Muted)
                        inner()
                    }
                )
                IconButton(onClick = {
                    try {
                        voice.launch(Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                        })
                    } catch (_: ActivityNotFoundException) {
                        imageError = "Thiết bị chưa hỗ trợ nhập bằng giọng nói."
                    }
                }) { Icon(Icons.Outlined.Mic, contentDescription = "Bắt đầu giọng nói") }
            }
        }
    }
    if (imageError.isNotBlank()) Text(imageError, color = MaterialTheme.colorScheme.error)
    Panel("Định dạng lời giải") {
        Choices(listOf("Văn bản", "Hình ảnh"), if (imageRequested) "Hình ảnh" else "Văn bản") {
            imageRequested = it == "Hình ảnh"
        }
    }
    val image = imageUri?.let(Uri::parse)
    val prompt = question.trim()
    AiAction("Gửi câu hỏi", enabled = image != null || prompt.isNotEmpty()) {
        val requestedFormat = imageRequested
        val answer = StudyAi.ask(apiKey, context, prompt, image, "Tự xác định môn học", "Tự xác định từ đề bài", style)
        sentQuestion = prompt.ifBlank { "Ảnh đề bài" }
        response = answer
        illustration = null
        responseAsImage = requestedFormat
        responseVersion++
        sample = false
        showAnswer(sentQuestion, answer, "Khác")
        updateQuestion("")
        imageUri = null
        if (requestedFormat) {
            generatingImage = true
            try {
                illustration = StudyAi.illustrate(apiKey, answer)
            } finally {
                generatingImage = false
            }
        }
    }
    TextButton(enabled = !generatingImage, onClick = {
        sample = true
        illustration = null
        sentQuestion = Example
        response = "Bước 1. Trừ 3 ở hai vế: 2x = 4.\n\nBước 2. Chia hai vế cho 2: x = 2."
        responseAsImage = imageRequested
        responseVersion++
    }) { Text("Xem lời giải mẫu") }
}

@Composable
internal fun QuestionImage(uri: Uri) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    val sample = (maxOf(info.size.width, info.size.height) / 512).coerceAtLeast(1)
                    decoder.setTargetSampleSize(sample)
                }
            }.getOrNull()
        }
    }
    thumbnail?.let { Image(it.asImageBitmap(), "Ảnh đề bài đã chọn", Modifier.fillMaxWidth().heightIn(max = 200.dp)) }
}

@Composable
internal fun AnswerCard(
    answer: String,
    title: String = "AI Mentor",
    startAsImage: Boolean = false,
    illustration: Bitmap? = null,
    generatingImage: Boolean = false,
    generateImage: (suspend () -> Bitmap)? = null
) {
    var asImage by rememberSaveable(answer, startAsImage) { mutableStateOf(startAsImage) }
    var generated by remember(answer) { mutableStateOf<Bitmap?>(null) }
    Panel(title) {
        Choices(listOf("Văn bản", "Hình ảnh"), if (asImage) "Hình ảnh" else "Văn bản") { asImage = it == "Hình ảnh" }
        if (asImage && generateImage != null) {
            val bitmap = illustration ?: generated
            when {
                bitmap != null -> AnswerImage(bitmap, "Ảnh minh họa do Vilao tạo")
                generatingImage -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Vilao đang tạo ảnh minh họa…")
                }
                else -> AiAction("Tạo ảnh minh họa") { generated = generateImage() }
            }
        } else if (asImage) {
            var tooLong by remember(answer) { mutableStateOf(false) }
            val bitmap by produceState<Bitmap?>(null, answer) {
                value = withContext(Dispatchers.Default) {
                    val paint = android.text.TextPaint(3).apply { color = 0xFF171C38.toInt(); textSize = 32f }
                    val layout = android.text.StaticLayout.Builder.obtain(answer, 0, answer.length, paint, 840)
                        .setLineSpacing(8f, 1f).build()
                    if (layout.height > 4000) return@withContext null
                    Bitmap.createBitmap(904, layout.height + 64, Bitmap.Config.ARGB_8888).apply {
                        val canvas = android.graphics.Canvas(this)
                        canvas.drawColor(0xFFE9E8FF.toInt())
                        canvas.translate(32f, 32f)
                        layout.draw(canvas)
                    }
                }
                tooLong = value == null
            }
            when {
                tooLong -> {
                    Text("Lời giải dài hơn giới hạn của thẻ ảnh.")
                    Text(answer)
                }
                bitmap == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                else -> bitmap?.let { AnswerImage(it, answer) }
            }
        } else {
            Text(answer)
        }
    }
}

@Composable
internal fun AnswerImage(bitmap: Bitmap, answer: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) {
            saving = true
            status = ""
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        } == true
                    } catch (_: IOException) {
                        false
                    } catch (_: SecurityException) {
                        false
                    }
                }
                saving = false
                status = if (saved) "Đã lưu ảnh PNG." else "Không lưu được ảnh. Hãy thử lại."
            }
        }
    }
    Image(bitmap.asImageBitmap(), answer, Modifier.fillMaxWidth())
    OutlinedButton(enabled = !saving, onClick = { save.launch("loi-giai-ai.png") }) {
        Text(if (saving) "Đang lưu…" else "Lưu ảnh PNG")
    }
    if (status.isNotEmpty()) Text(status)
}
