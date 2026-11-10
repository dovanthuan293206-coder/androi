package com.example.aistudymentor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.aistudymentor.data.StudyAi
import com.example.aistudymentor.data.StudyCloud
import com.example.aistudymentor.data.StudyEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

private val Indigo = Color(0xFF4543AB)
private val Ink = Color(0xFF171C38)
private val Canvas = Color(0xFFF6F7FC)
private val Lilac = Color(0xFFE9E8FF)
private val subjects = listOf("Toán", "Vật lý", "Hóa học", "Sinh học")
private val levels = listOf("THCS", "THPT", "Đại học")
private val styles = listOf("Dễ hiểu", "Chi tiết", "Từng bước")
private const val Example = "Giải phương trình 2x + 3 = 7"

private enum class Screen(val title: String, val icon: String = "") {
    Login("Đăng nhập"), Preferences("Thiết lập học tập"),
    Home("Trang chủ", "⌂"), Question("Đặt câu hỏi"), Answer("Lời giải"),
    History("Lịch sử", "◷"), Bookmarks("Đã lưu"), Quiz("Luyện tập"),
    Progress("Tiến độ", "▥"), Profile("Cá nhân", "◎")
}

private val tabs = listOf(Screen.Home, Screen.History, Screen.Progress, Screen.Profile)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Indigo, onPrimary = Color.White,
                    primaryContainer = Lilac, onPrimaryContainer = Ink,
                    background = Canvas, onBackground = Ink, surface = Color.White,
                    onSurface = Ink, secondaryContainer = Lilac
                )
            ) {
                StudyMentor { answer ->
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, answer)
                    }, null))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyMentor(share: (String) -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    val account = user
    if (account == null) {
        Scaffold(containerColor = Canvas) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) { LoginPanel(auth) }
        }
    } else key(account.uid) {
        AccountStudyMentor(account.uid, account.email.orEmpty(), auth::signOut, share)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountStudyMentor(uid: String, email: String, signOut: () -> Unit, share: (String) -> Unit) {
    val cloud = remember(uid) { StudyCloud(uid) }
    var entries by remember { mutableStateOf(emptyList<StudyEntry>()) }
    var cloudError by remember { mutableStateOf("") }
    val writeFailed = { cloudError = "Firebase chưa lưu được dữ liệu. Kiểm tra kết nối và quyền Firestore." }
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var parent by rememberSaveable { mutableStateOf(Screen.Home) }
    var answerBack by rememberSaveable { mutableStateOf(Screen.Home) }
    var question by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf("THPT") }
    var style by rememberSaveable { mutableStateOf("Từng bước") }
    var selectedSubjects by rememberSaveable { mutableStateOf(arrayListOf("Toán")) }
    var aiAnswer by rememberSaveable { mutableStateOf("") }
    var vilaoKey by remember { mutableStateOf("") }
    var answerQuestion by rememberSaveable { mutableStateOf("") }
    var activeEntryId by rememberSaveable { mutableStateOf("") }
    DisposableEffect(cloud) {
        val readFailed = { cloudError = "Không đọc được dữ liệu Firestore. Kiểm tra cấu hình dự án và quyền truy cập." }
        val profile = cloud.observeProfile({ savedLevel, savedStyle, savedSubjects ->
            level = savedLevel
            style = savedStyle
            selectedSubjects = ArrayList(savedSubjects)
        }, readFailed)
        val history = cloud.observeEntries({ entries = it }, readFailed)
        onDispose {
            profile.remove()
            history.remove()
        }
    }
    fun open(next: Screen) {
        parent = screen
        screen = next
    }
    fun example() {
        aiAnswer = ""
        answerBack = screen
        screen = Screen.Answer
    }
    fun showEntry(entry: StudyEntry) {
        activeEntryId = entry.id
        answerQuestion = entry.question
        aiAnswer = entry.content
        answerBack = screen
        screen = Screen.Answer
    }
    fun back() {
        screen = when (screen) {
            Screen.Answer -> answerBack
            in tabs -> Screen.Home
            else -> parent
        }
    }
    BackHandler(screen != Screen.Login && screen != Screen.Home) { back() }
    Scaffold(
        containerColor = Canvas,
        topBar = {
            if (screen != Screen.Login) {
                TopAppBar(
                    title = { Text(screen.title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (screen !in tabs) TextButton(onClick = { back() }) { Text("‹  Quay lại") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas)
                )
            }
        },
        bottomBar = {
            if (screen in tabs || screen == Screen.Bookmarks || screen == Screen.Quiz) {
                NavigationBar(containerColor = Color.White) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = screen == tab,
                            onClick = { screen = tab },
                            icon = { Text(tab.icon, style = MaterialTheme.typography.titleLarge) },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        key(screen) {
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (cloudError.isNotBlank()) {
                    Text(cloudError, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { cloudError = "" }) { Text("Đóng thông báo") }
                }
                when (screen) {
                    Screen.Login -> Unit
                    Screen.Preferences -> {
                        Heading("Cá nhân hóa cách học", "Chọn cấp học, môn quan tâm và cách bạn muốn đọc lời giải.")
                        Panel("Cấp học") { Choices(levels, level) { level = it } }
                        Panel("Môn quan tâm") {
                            subjects.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { subject ->
                                        FilterChip(subject in selectedSubjects, {
                                            selectedSubjects = ArrayList(
                                                if (subject in selectedSubjects) selectedSubjects - subject
                                                else selectedSubjects + subject
                                            )
                                        }, { Text(subject) })
                                    }
                                }
                            }
                        }
                        Panel("Cách giải thích") {
                            styles.forEach { option ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(style == option, { style = option })
                                    Text(option)
                                }
                            }
                        }
                        Action("Lưu thiết lập") {
                            cloud.saveProfile(level, style, selectedSubjects.toList(), writeFailed)
                            screen = parent
                        }
                    }
                    Screen.Home -> {
                        Hero("Chào bạn!", "Sẵn sàng học điều mới?", "Bắt đầu với một câu hỏi hoặc thử xem cách ứng dụng trình bày lời giải.")
                        Action("Đặt câu hỏi") { open(Screen.Question) }
                        ShortcutRow(Screen.Quiz, Screen.History) { open(it) }
                        ShortcutRow(Screen.Bookmarks, Screen.Progress) { open(it) }
                        Panel("Câu hỏi gần đây") {
                            if (entries.isEmpty()) Text("Các câu hỏi và bài luyện tập sẽ xuất hiện ở đây.")
                            entries.take(3).forEach { entry ->
                                TextButton(onClick = { showEntry(entry) }) { Text(entry.question) }
                            }
                        }
                    }
                    Screen.Question -> QuestionPanel(
                        question = question,
                        updateQuestion = { question = it },
                        preferredLevel = level,
                        style = style,
                        apiKey = vilaoKey,
                        showExample = { example() },
                        showAnswer = { prompt, answer, subject ->
                            activeEntryId = cloud.add("answer", prompt, answer, subject, writeFailed)
                            answerQuestion = prompt
                            aiAnswer = answer
                            answerBack = Screen.Question
                            screen = Screen.Answer
                        }
                    )
                    Screen.Answer if aiAnswer.isNotEmpty() -> {
                        Panel("Đề bài") { Text(answerQuestion) }
                        val entry = entries.find { it.id == activeEntryId }
                        if (entry?.kind == "quiz") {
                            QuizResult(JSONObject(entry.content), entry.selection ?: -1) {
                                cloud.select(entry.id, it, writeFailed)
                            }
                        } else {
                            Panel("Lời giải từ Vilao AI") { Text(aiAnswer) }
                            OutlinedButton(onClick = { share(aiAnswer) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Chia sẻ lời giải")
                            }
                        }
                        if (entry != null) {
                            OutlinedButton(
                                onClick = { cloud.bookmark(entry.id, !entry.bookmarked, writeFailed) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (entry.bookmarked) "Bỏ đánh dấu" else "Đánh dấu để ôn tập") }
                        }
                        Action("Đặt câu hỏi mới") {
                            question = ""
                            screen = Screen.Question
                        }
                    }
                    Screen.Answer -> {
                        Hero("BÀI MẪU · TOÁN", "x = 2", Example)
                        Panel("Giải từng bước") {
                            Text("1. Trừ 3 ở cả hai vế: 2x = 4.")
                            Text("2. Chia cả hai vế cho 2: x = 2.")
                            Text("3. Thử lại: 2 × 2 + 3 = 7.")
                        }
                        OutlinedButton(onClick = { share("$Example\nTrừ 3 ở hai vế: 2x = 4. Chia hai vế cho 2: x = 2.") },
                            modifier = Modifier.fillMaxWidth()) { Text("Chia sẻ lời giải") }
                    }
                    Screen.History -> {
                        var search by rememberSaveable { mutableStateOf("") }
                        var filter by rememberSaveable { mutableStateOf("Tất cả") }
                        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(),
                            label = { Text("Tìm câu hỏi") }, singleLine = true)
                        Choices(listOf("Tất cả", "Toán", "Khoa học"), filter) { filter = it }
                        val results = entries.filter { entry ->
                            entry.question.contains(search.trim(), ignoreCase = true) && when (filter) {
                                "Toán" -> entry.subject == "Toán"
                                "Khoa học" -> entry.subject in subjects.drop(1)
                                else -> true
                            }
                        }
                        EntryList(results, "Chưa có câu hỏi phù hợp", ::showEntry)
                    }
                    Screen.Bookmarks -> {
                        EntryList(entries.filter { it.bookmarked }, "Chưa có bài đã lưu", ::showEntry)
                    }
                    Screen.Quiz -> QuizPanel(level, vilaoKey, create = { subject, quiz ->
                        cloud.add("quiz", JSONObject(quiz).getString("question"), quiz, subject, writeFailed)
                    }, select = { id, choice -> cloud.select(id, choice, writeFailed) })
                    Screen.Progress -> {
                        var view by rememberSaveable { mutableStateOf("Tổng quan") }
                        Heading("Nhịp học của bạn", "Thống kê từ lịch sử của tài khoản.")
                        Choices(listOf("Tổng quan", "Theo môn"), view) { view = it }
                        if (view == "Tổng quan") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Metric(entries.count { it.kind == "answer" }.toString(), "Hỏi đáp", Modifier.weight(1f))
                                Metric(entries.count { it.selection != null }.toString(), "Quiz đã làm", Modifier.weight(1f))
                                Metric((entries.count { it.selection != null } * 10).toString(), "XP", Modifier.weight(1f))
                            }
                        } else subjects.forEach { subject ->
                            Panel(subject) { Text("${entries.count { it.subject == subject }} hoạt động") }
                        }
                    }
                    Screen.Profile -> {
                        Hero("HỒ SƠ", email, "Cấp học · $level")
                        Panel("Kết nối Vilao AI") {
                            OutlinedTextField(
                                value = vilaoKey,
                                onValueChange = { vilaoKey = it.trim() },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Khóa API Vilao") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                            Text(
                                if (BuildConfig.VILAO_API_KEY.isNotBlank()) "Bản debug đã có khóa mặc định. Nhập ở đây để dùng khóa khác trong phiên này."
                                else "Khóa nhập ở đây được giữ trong phiên hiện tại."
                            )
                        }
                        Panel("Tùy chọn của bạn") {
                            Text(selectedSubjects.joinToString().ifEmpty { "Chưa chọn môn học" })
                            Text("Cách giải thích: $style")
                            TextButton(onClick = { open(Screen.Preferences) }) { Text("Chỉnh thiết lập  ›") }
                            TextButton(onClick = { open(Screen.Bookmarks) }) { Text("Xem bài đã lưu  ›") }
                        }
                        OutlinedButton(onClick = signOut, modifier = Modifier.fillMaxWidth()) { Text("Đăng xuất") }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginPanel(auth: FirebaseAuth) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Spacer(Modifier.height(16.dp))
    Hero("TRỢ LÝ HỌC TẬP", "AI Study Mentor", "Học theo từng bước, theo cách phù hợp với bạn.")
    Panel(if (register) "Tạo tài khoản" else "Chào mừng trở lại") {
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(),
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(),
            label = { Text("Mật khẩu") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        TextButton(enabled = !busy, onClick = { register = !register; password = ""; error = "" }) {
            Text(if (register) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký")
        }
    }
    Button(
        enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        onClick = {
            busy = true
            error = ""
            val task = if (register) auth.createUserWithEmailAndPassword(email.trim(), password)
                else auth.signInWithEmailAndPassword(email.trim(), password)
            task.addOnCompleteListener {
                busy = false
                if (!it.isSuccessful) {
                    error = when ((it.exception as? FirebaseAuthException)?.errorCode) {
                        "ERROR_WEAK_PASSWORD" -> "Mật khẩu chưa đủ mạnh."
                        "ERROR_EMAIL_ALREADY_IN_USE" -> "Email này đã có tài khoản."
                        "ERROR_OPERATION_NOT_ALLOWED" -> "Hãy bật phương thức Email/Password trong Firebase Authentication."
                        else -> "Không thể xác thực. Kiểm tra email, mật khẩu và kết nối mạng."
                    }
                }
            }
        }
    ) { Text(if (busy) "Đang xác thực…" else if (register) "Đăng ký" else "Đăng nhập") }
    if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun QuestionPanel(
    question: String,
    updateQuestion: (String) -> Unit,
    preferredLevel: String,
    style: String,
    apiKey: String,
    showExample: () -> Unit,
    showAnswer: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf("Văn bản") }
    var subject by rememberSaveable { mutableStateOf("Toán") }
    var level by rememberSaveable { mutableStateOf(preferredLevel) }
    var imageUri by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri?.toString() ?: imageUri
    }
    Heading("Bạn đang vướng ở đâu?", "Soạn câu hỏi hoặc chọn ảnh đề bài.")
    Choices(listOf("Văn bản", "Hình ảnh"), mode) { mode = it }
    Panel("Nội dung câu hỏi") {
        if (mode == "Văn bản") {
            OutlinedTextField(question, { if (it.length <= 1000) updateQuestion(it) },
                Modifier.fillMaxWidth(), minLines = 4, label = { Text("Nhập đề bài") },
                supportingText = { Text("${question.length}/1000") })
        } else {
            OutlinedButton(onClick = { picker.launch("image/*") }) {
                Text(if (imageUri == null) "Chọn ảnh đề bài" else "Đã chọn ảnh · Chọn lại")
            }
        }
    }
    Panel("Môn học") {
        subjects.chunked(2).forEach { Choices(it, subject) { value -> subject = value } }
    }
    Panel("Cấp học") { Choices(levels, level) { level = it } }
    val prompt = if (mode == "Văn bản") question.trim() else "Ảnh đề bài"
    val image = if (mode == "Hình ảnh") imageUri?.let(Uri::parse) else null
    AiAction("Nhận lời giải", enabled = if (mode == "Hình ảnh") image != null else prompt.isNotEmpty()) {
        val answer = StudyAi.ask(apiKey, context, if (image == null) prompt else "", image, subject, level, style)
        showAnswer(prompt, answer, subject)
    }
    TextButton(onClick = showExample) { Text("Xem lời giải mẫu") }
}

@Composable
private fun QuizPanel(level: String, apiKey: String, create: (String, String) -> String, select: (String, Int) -> Unit) {
    var subject by rememberSaveable { mutableStateOf("Toán") }
    var difficulty by rememberSaveable { mutableStateOf("Dễ") }
    var quizJson by rememberSaveable { mutableStateOf("") }
    var quizId by rememberSaveable { mutableStateOf("") }
    var answer by rememberSaveable { mutableIntStateOf(-1) }
    Heading("Luyện nhanh", "Vilao tạo câu hỏi trắc nghiệm theo môn và độ khó cho cấp $level.")
    Panel("Môn học") {
        subjects.chunked(2).forEach { Choices(it, subject) { value -> subject = value } }
    }
    Panel("Độ khó") { Choices(listOf("Dễ", "Vừa", "Khó"), difficulty) { difficulty = it } }
    AiAction(if (quizJson.isEmpty()) "Tạo câu hỏi" else "Tạo câu hỏi mới") {
        val generated = StudyAi.quiz(apiKey, subject, level, difficulty)
        quizId = create(subject, generated)
        quizJson = generated
        answer = -1
    }
    if (quizJson.isNotEmpty()) {
        val quiz = remember(quizJson) { JSONObject(quizJson) }
        QuizResult(quiz, answer) {
            answer = it
            select(quizId, it)
        }
    }
}

@Composable
private fun EntryList(entries: List<StudyEntry>, empty: String, open: (StudyEntry) -> Unit) {
    if (entries.isEmpty()) Text(empty)
    entries.forEach { entry ->
        val kind = if (entry.kind == "quiz") "Luyện tập" else "Hỏi đáp"
        Panel("${entry.subject} · $kind") {
            TextButton(onClick = { open(entry) }) { Text(entry.question) }
        }
    }
}

@Composable
private fun QuizResult(quiz: JSONObject, answer: Int, select: (Int) -> Unit) {
        val options = quiz.getJSONArray("options")
        val correct = quiz.getInt("correctIndex")
        Panel(quiz.getString("question")) {
            repeat(options.length()) { index ->
                OutlinedButton(
                    onClick = { select(index) },
                    enabled = answer == -1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${'A' + index}. ${options.getString(index)}")
                }
            }
        }
        if (answer >= 0) {
            Panel(if (answer == correct) "Chính xác!" else "Đáp án đúng là ${'A' + correct}") {
                Text(quiz.getString("explanation"))
            }
        }
}

@Composable
private fun AiAction(label: String, enabled: Boolean = true, run: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    Button(
        onClick = {
            if (!busy) {
                busy = true
                message = ""
                scope.launch {
                    try {
                        run()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: IOException) {
                        message = "Không kết nối được Vilao. Hãy kiểm tra Internet và thử lại."
                    } catch (error: Exception) {
                        message = error.message ?: "Yêu cầu thất bại. Hãy thử lại."
                    } finally {
                        busy = false
                    }
                }
            }
        },
        enabled = enabled && !busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    ) { Text(if (busy) "Vilao đang xử lý…" else label) }
    if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun Heading(title: String, description: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Hero(kicker: String, title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Indigo, contentColor = Color.White),
        shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(kicker, color = Lilac, style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(detail, color = Lilac)
        }
    }
}

@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun Action(label: String, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(label) }
}

@Composable
private fun Choices(options: List<String>, selected: String, choose: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> FilterChip(option == selected, { choose(option) }, { Text(option) }) }
    }
}

@Composable
private fun ShortcutRow(first: Screen, second: Screen, select: (Screen) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(first, second).forEach { target ->
            Card(onClick = { select(target) }, modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (target.icon.isEmpty()) "+" else target.icon,
                        Modifier.background(Lilac, MaterialTheme.shapes.small).padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Indigo, style = MaterialTheme.typography.titleLarge)
                    Text(target.title, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = Indigo)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
