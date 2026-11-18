package com.example.aistudymentor

import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudymentor.data.StudyAi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
internal fun QuizPanel(level: String, apiKey: String, scroll: ScrollState, create: (String, String) -> String, select: (String, Int) -> Unit) {
    var quick by rememberSaveable { mutableStateOf(false) }
    var subject by rememberSaveable { mutableStateOf("Toán") }
    var topic by rememberSaveable { mutableStateOf("") }
    var countText by rememberSaveable { mutableStateOf("10") }
    val count = countText.toIntOrNull()
    var quizJson by rememberSaveable { mutableStateOf("") }
    var entryIds by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var answers by rememberSaveable { mutableStateOf(arrayListOf<Int>()) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var review by rememberSaveable { mutableStateOf(false) }

    fun reset() {
        quizJson = ""
        entryIds = arrayListOf()
        answers = arrayListOf()
        index = 0
        finished = false
        review = false
    }
    fun start(quiz: org.json.JSONArray, ids: ArrayList<String>) {
        quizJson = quiz.toString()
        entryIds = ids
        answers = ArrayList(List(quiz.length()) { -1 })
        index = 0
    }
    BackHandler(quizJson.isNotEmpty()) {
        if (review) review = false
        else if (!finished && index > 0) index--
        else reset()
    }
    if (quizJson.isEmpty()) {
        Choices(listOf("Bộ câu hỏi", "Quiz nhanh"), if (quick) "Quiz nhanh" else "Bộ câu hỏi") {
            quick = it == "Quiz nhanh"
        }
        Panel("Môn học") {
            subjects.chunked(2).forEach { Choices(it, subject) { value -> subject = value } }
        }
        OutlinedTextField(topic, { if (it.length <= 150) topic = it }, Modifier.fillMaxWidth(),
            label = { Text("Chủ đề") }, placeholder = { Text("Ví dụ: Phương trình bậc hai") }, singleLine = true)
        if (!quick) {
            Panel("Số câu hỏi") {
                OutlinedTextField(
                    value = countText,
                    onValueChange = { value -> if (value.length <= 4 && value.all(Char::isDigit)) countText = value },
                    label = { Text("Nhập số câu hỏi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = count == null || count < 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Panel("Dạng câu hỏi") { Text("Trắc nghiệm · 4 lựa chọn") }
        val total = if (quick) 5 else count ?: 0
        AiAction(if (quick) "Bắt đầu Quiz nhanh" else "Tạo bộ câu hỏi", enabled = total > 0) {
            val quiz = StudyAi.quiz(apiKey, subject, level, topic.trim(), total)
            val ids = ArrayList((0 until quiz.length()).map { create(subject, quiz.getJSONObject(it).toString()) })
            start(quiz, ids)
        }
        OutlinedButton(enabled = total > 0, onClick = { start(sampleQuiz(total), arrayListOf()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Thử quiz mẫu · Phương trình bậc hai")
        }
        return
    }
    val items = remember(quizJson) { org.json.JSONArray(quizJson) }
    LaunchedEffect(index, finished, review) { scroll.scrollTo(0) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (finished) {
            val correct = answers.indices.count { answers[it] == items.getJSONObject(it).getInt("correctIndex") }
            val percent = correct * 100 / items.length()
            Hero("KẾT QUẢ QUIZ", "Hoàn thành!", "$correct/${items.length()} câu đúng ($percent%)")
            TextButton(onClick = { review = !review }) { Text(if (review) "Ẩn đáp án" else "Xem đáp án") }
            if (review) {
                repeat(items.length()) { row ->
                    val item = items.getJSONObject(row)
                    val right = item.getInt("correctIndex")
                    Panel("${row + 1}. ${item.getString("question")}") {
                        val chosen = answers[row]
                        if (chosen >= 0) Text("Bạn chọn: ${item.getJSONArray("options").getString(chosen)}")
                        Text("Đáp án đúng: ${item.getJSONArray("options").getString(right)}")
                        Text(item.getString("explanation"))
                    }
                }
            }
            Action("Làm lại") {
                if (entryIds.isNotEmpty()) {
                    entryIds = ArrayList((0 until items.length()).map { create(subject, items.getJSONObject(it).toString()) })
                }
                answers = ArrayList(List(items.length()) { -1 })
                index = 0
                finished = false
                review = false
            }
            OutlinedButton(onClick = { reset() }, modifier = Modifier.fillMaxWidth()) { Text("Tạo bài khác") }
        } else {
            Text("Câu ${index + 1}/${items.length()}", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { (index + 1f) / items.length() }, modifier = Modifier.fillMaxWidth())
            val item = items.getJSONObject(index)
            val options = item.getJSONArray("options")
            Panel(item.getString("question")) {
                repeat(options.length()) { option ->
                    val chosen = answers[index] == option
                    OutlinedButton(
                        onClick = { answers = ArrayList(answers).apply { set(index, option) } },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (chosen) Lilac else Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("${'A' + option}. ${options.getString(option)}") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(enabled = index > 0, onClick = { index-- }) { Text("‹ Trước") }
                Button(onClick = {
                    if (index + 1 < items.length()) index++
                    else {
                        answers.forEachIndexed { row, choice -> entryIds.getOrNull(row)?.let { select(it, choice) } }
                        finished = true
                    }
                }, enabled = answers[index] >= 0) {
                    Text(if (index + 1 == items.length()) "Nộp bài" else "Tiếp ›")
                }
            }
        }
    }
}

internal fun sampleQuiz(count: Int): org.json.JSONArray = org.json.JSONArray().apply {
    repeat(count) { index ->
        val root = index + 2
        val correct = index % 4
        val options = mutableListOf("x = $root", "x = −$root", "x = ${root * root}")
        options.add(correct, "x = ±$root")
        put(JSONObject().put("question", "Nghiệm của phương trình x² − ${root * root} = 0 là gì?")
            .put("options", org.json.JSONArray(options)).put("correctIndex", correct)
            .put("explanation", "Ta có x² = ${root * root}, suy ra x = $root hoặc x = −$root."))
    }
}
