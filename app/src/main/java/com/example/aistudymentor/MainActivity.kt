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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private val subjects = listOf("Toán", "Vật lý", "Hóa học", "Sinh học", "Lập trình", "Lịch sử", "Ngôn ngữ", "Khác")
private val levels = listOf("Tiểu học", "THCS", "THPT", "Đại học")
private val styles = listOf("Ngắn gọn", "Chi tiết", "Từng bước")
private const val Example = "Giải phương trình 2x + 3 = 7"

private enum class Screen(val title: String, val icon: String = "") {
    Login("Đăng nhập"), Preferences("Thiết lập học tập"),
    Home("Trang chủ", "⌂"), Question("Đặt câu hỏi"), Answer("Lời giải"),
    History("Lịch sử", "◷"), Bookmarks("Đã lưu"), Quiz("Luyện tập"),
    Progress("Tiến độ", "▥"), Profile("Cá nhân", "◎"),
    Achievements("Thành tích & Huy hiệu"), Settings("Cài đặt"), Help("Trợ giúp & Phản hồi")
}

private val tabs = listOf(Screen.Home, Screen.Question, Screen.Quiz, Screen.History, Screen.Progress)

private data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem(Screen.Home, "Home", Icons.Outlined.Home),
    NavItem(Screen.Question, "Hỏi AI", Icons.Outlined.ChatBubbleOutline),
    NavItem(Screen.Quiz, "Luyện tập", Icons.Outlined.Edit),
    NavItem(Screen.History, "Thư viện", Icons.Outlined.Book),
    NavItem(Screen.Progress, "Tiến độ", Icons.Outlined.BarChart)
)

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
    var showWelcome by rememberSaveable { mutableStateOf(true) }
    if (showWelcome) {
        WelcomeScreen { showWelcome = false }
        return
    }
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    val account = user
    if (account == null) {
        LoginScreen(auth)
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
    val answerCount = entries.count { it.kind == "answer" }
    val quizDone = entries.count { it.selection != null }
    val xp = answerCount * 5 + quizDone * 10
    val levelNo = xp / 200 + 1
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
    if (screen == Screen.Preferences) {
        PreferencesWizard(
            initialLevel = level,
            initialSubjects = selectedSubjects,
            initialStyle = style,
            save = { newLevel, newSubjects, newStyle ->
                level = newLevel
                style = newStyle
                selectedSubjects = ArrayList(newSubjects)
                cloud.saveProfile(newLevel, newStyle, newSubjects, writeFailed)
            },
            exit = { screen = parent }
        )
        return
    }
    Scaffold(
        containerColor = Canvas,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screen.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (screen !in tabs && screen != Screen.Profile) {
                        IconButton(onClick = { back() }) { Text("←", style = MaterialTheme.typography.titleLarge) }
                    }
                },
                actions = {
                    if (screen == Screen.Profile) {
                        IconButton(onClick = { open(Screen.Settings) }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Cài đặt")
                        }
                    } else if (screen in tabs) {
                        IconButton(onClick = { open(Screen.Profile) }) {
                            Icon(Icons.Outlined.Person, contentDescription = "Cá nhân")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas)
            )
        },
        bottomBar = {
            if (screen in tabs || screen == Screen.Profile || screen == Screen.Bookmarks) {
                BottomNav(screen) { screen = it }
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
                    Screen.Preferences -> Unit
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
                                "Khoa học" -> entry.subject in listOf("Vật lý", "Hóa học", "Sinh học")
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
                                Metric(answerCount.toString(), "Hỏi đáp", Modifier.weight(1f))
                                Metric(quizDone.toString(), "Quiz đã làm", Modifier.weight(1f))
                                Metric(xp.toString(), "XP", Modifier.weight(1f))
                            }
                        } else subjects.forEach { subject ->
                            Panel(subject) { Text("${entries.count { it.subject == subject }} hoạt động") }
                        }
                    }
                    Screen.Profile -> {
                        var leaderboardNotice by remember { mutableStateOf(false) }
                        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Bạn" }
                        val levelStart = (levelNo - 1) * 200
                        ProfileHeader(name, email, levelNo, xp, levelNo * 200, (xp - levelStart) / 200f)
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDEBFF)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    Modifier.size(44.dp).background(Color.White, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) { Text("👑", style = MaterialTheme.typography.titleLarge) }
                                Column {
                                    Text("Gói học", fontWeight = FontWeight.Bold)
                                    Text("Gói miễn phí · Cấp học $level", color = Muted,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Column {
                            ProfileRow(Icons.Outlined.Person, "Hồ sơ & Thiết lập học tập") { open(Screen.Preferences) }
                            ProfileRow(Icons.Outlined.EmojiEvents, "Thành tích & Huy hiệu") { open(Screen.Achievements) }
                            ProfileRow(Icons.Outlined.Leaderboard, "Bảng xếp hạng") { leaderboardNotice = true }
                            ProfileRow(Icons.Outlined.Settings, "Cài đặt") { open(Screen.Settings) }
                            ProfileRow(Icons.Outlined.ErrorOutline, "Trợ giúp & Phản hồi") { open(Screen.Help) }
                            ProfileRow(Icons.AutoMirrored.Outlined.Logout, "Đăng xuất", onClick = signOut)
                        }
                        if (leaderboardNotice) MessageBox("Bảng xếp hạng sắp ra mắt.", false)
                    }
                    Screen.Achievements -> {
                        Heading("Huy hiệu của bạn", "Hoàn thành mục tiêu để mở khóa huy hiệu mới.")
                        listOf(
                            Triple("Câu hỏi đầu tiên", "Đặt 1 câu hỏi cho Vilao", answerCount >= 1),
                            Triple("Hỏi đáp chăm chỉ", "Đặt 10 câu hỏi", answerCount >= 10),
                            Triple("Khởi động luyện tập", "Làm 1 câu trắc nghiệm", quizDone >= 1),
                            Triple("Cao thủ trắc nghiệm", "Làm 10 câu trắc nghiệm", quizDone >= 10),
                            Triple("Nhà sưu tầm", "Đánh dấu 3 bài để ôn tập", entries.count { it.bookmarked } >= 3),
                            Triple("Lên cấp 2", "Đạt 200 XP", levelNo >= 2)
                        ).forEach { (title, detail, done) ->
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(if (done) "🏅" else "🔒", style = MaterialTheme.typography.headlineSmall)
                                    Column(Modifier.weight(1f)) {
                                        Text(title, fontWeight = FontWeight.Bold)
                                        Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (done) Text("Đã đạt", color = Good, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    Screen.Settings -> {
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
                            Text("Nhập khóa Vilao riêng của bạn. App giữ khóa trong phiên hiện tại.")
                        }
                        Panel("Tùy chọn học tập") {
                            Text("Cấp học: $level")
                            Text(selectedSubjects.joinToString().ifEmpty { "Chưa chọn môn học" })
                            Text("Cách giải thích: $style")
                            TextButton(onClick = { open(Screen.Preferences) }) { Text("Chỉnh thiết lập  ›") }
                        }
                        Panel("Tài khoản") {
                            Text(email)
                            Text("AI Study Mentor · Phiên bản 1.0", color = Muted)
                        }
                    }
                    Screen.Help -> {
                        Panel("Cách dùng nhanh") {
                            Text("• Nhập khóa Vilao trong Cài đặt trước khi hỏi AI hoặc tạo bài luyện tập.")
                            Text("• Với ảnh đề bài, chọn ảnh JPG, PNG, WebP hoặc GIF nhỏ hơn 4 MB.")
                            Text("• Lịch sử, bài đã đánh dấu và tiến độ được lưu theo tài khoản của bạn.")
                        }
                        Panel("Gặp sự cố?") {
                            Text("Kiểm tra kết nối Internet, số dư tài khoản Vilao và quyền Firestore nếu app báo lỗi lưu dữ liệu.")
                        }
                    }
                }
            }
        }
    }
}

private val FieldFill = Color(0xFFF3F2FB)
private val FieldBorder = Color(0xFFE2E1F2)
private val Muted = Color(0xFF7B7D98)
private val Good = Color(0xFF2E9E5B)

private fun authError(error: Exception?): String = when ((error as? FirebaseAuthException)?.errorCode) {
    "ERROR_WEAK_PASSWORD" -> "Mật khẩu chưa đủ mạnh."
    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email này đã có tài khoản."
    "ERROR_INVALID_EMAIL" -> "Email chưa đúng định dạng."
    "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "Email hoặc mật khẩu chưa đúng."
    "ERROR_OPERATION_NOT_ALLOWED" -> "Hãy bật phương thức Email/Password trong Firebase Authentication."
    else -> "Không thể xác thực. Kiểm tra email, mật khẩu và kết nối mạng."
}

@Composable
private fun LoginScreen(auth: FirebaseAuth) {
    var register by rememberSaveable { mutableStateOf(false) }
    BackHandler(register) { register = false }
    Box(Modifier.fillMaxSize().background(Color.White)) {
        AnimatedContent(
            register, label = "auth",
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }
        ) { isRegister ->
            if (isRegister) RegisterForm(auth) { register = false }
            else LoginForm(auth) { register = true }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    leading: String? = null,
    secret: Boolean = false
) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Muted) },
        singleLine = true,
        leadingIcon = if (leading != null) { { Text(leading) } } else null,
        trailingIcon = if (secret) {
            { Text(if (show) "🙈" else "👁", Modifier.clickable { show = !show }.padding(12.dp)) }
        } else null,
        visualTransformation = if (secret && !show) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldFill, unfocusedContainerColor = FieldFill,
            focusedBorderColor = Indigo, unfocusedBorderColor = FieldBorder
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (secret) KeyboardType.Password else keyboardType, imeAction = imeAction
        )
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge, color = Ink)
}

@Composable
private fun MessageBox(text: String, isError: Boolean) {
    Box(
        Modifier.fillMaxWidth()
            .background(if (isError) Color(0xFFFDECEC) else Lilac, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) { Text(text, color = if (isError) Color(0xFFB3261E) else Ink) }
}

@Composable
private fun SocialButton(icon: String, iconColor: Color, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FieldBorder)
    ) {
        Text(icon, color = iconColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(10.dp))
        Text(label, color = Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LoginForm(auth: FirebaseAuth, goRegister: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(true) }
    val floating = rememberInfiniteTransition(label = "floating")
    val lift by floating.animateFloat(
        0f, -8f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lift"
    )
    fun submit() {
        if (busy || email.isBlank() || password.isEmpty()) return
        busy = true
        message = ""
        isError = true
        auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener {
            busy = false
            if (!it.isSuccessful) message = authError(it.exception)
        }
    }
    fun resetPassword() {
        if (busy) return
        isError = true
        if (email.isBlank()) {
            message = "Nhập email của bạn ở trên để nhận liên kết đặt lại mật khẩu."
            return
        }
        busy = true
        message = ""
        auth.sendPasswordResetEmail(email.trim()).addOnCompleteListener {
            busy = false
            isError = !it.isSuccessful
            message = if (it.isSuccessful) "Nếu email đã đăng ký, liên kết đặt lại mật khẩu đã được gửi."
            else authError(it.exception)
        }
    }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painterResource(R.drawable.study_ai),
            contentDescription = "Trợ lý AI Study Mentor",
            modifier = Modifier.fillMaxWidth(0.55f).height(130.dp).offset(y = lift.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            "AI Study Mentor", fontWeight = FontWeight.Bold, color = Ink,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            "Đăng nhập để tiếp tục hành trình học của bạn.", color = Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        AuthField(email, { email = it }, "Email", KeyboardType.Email, ImeAction.Next, leading = "✉")
        AuthField(password, { password = it }, "Mật khẩu", KeyboardType.Password, ImeAction.Done,
            leading = "🔒", secret = true)
        AnimatedVisibility(message.isNotEmpty()) { MessageBox(message, isError) }
        Button(
            onClick = { submit() },
            enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text(if (busy) "Đang xử lý…" else "Đăng nhập", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "Quên mật khẩu?", color = Ink, fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(enabled = !busy) { resetPassword() }.padding(8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = FieldBorder)
            Text("HOẶC", Modifier.padding(horizontal = 12.dp), color = Muted,
                style = MaterialTheme.typography.labelMedium)
            HorizontalDivider(Modifier.weight(1f), color = FieldBorder)
        }
        SocialButton("G", Color(0xFF4285F4), "Tiếp tục với Google") {
            isError = false
            message = "Đăng nhập bằng Google sắp được hỗ trợ."
        }
        SocialButton("🍎", Ink, "Tiếp tục với Apple") {
            isError = false
            message = "Đăng nhập bằng Apple sắp được hỗ trợ."
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Chưa có tài khoản? ", color = Muted)
            Text(
                "Đăng ký", color = Indigo, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !busy) { goRegister() }.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun RuleRow(text: String, ok: Boolean) {
    val color by animateColorAsState(if (ok) Good else Color(0xFFB8B9CC), label = "rule")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(20.dp).border(1.5.dp, color, CircleShape), contentAlignment = Alignment.Center) {
            Text("✓", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(text, color = if (ok) Ink else Muted)
    }
}

@Composable
private fun RegisterForm(auth: FirebaseAuth, goLogin: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val rules = listOf(
        "Ít nhất 8 ký tự" to (password.length >= 8),
        "Có chữ và số" to (password.any { it.isLetter() } && password.any { it.isDigit() }),
        "Có ký tự đặc biệt" to password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    )
    val matches = confirm.isNotEmpty() && confirm == password
    val valid = email.isNotBlank() && rules.all { it.second } && matches
    fun submit() {
        if (busy || !valid) return
        busy = true
        error = ""
        auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener {
            busy = false
            if (!it.isSuccessful) error = authError(it.exception)
        }
    }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "←", color = Indigo, style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.CenterStart)
                    .clickable(enabled = !busy) { goLogin() }.padding(8.dp)
            )
            Text(
                "Tạo tài khoản", fontWeight = FontWeight.Bold, color = Ink,
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(Modifier.height(8.dp))
        FieldLabel("Email")
        AuthField(email, { email = it }, "vidu@email.com", KeyboardType.Email, ImeAction.Next)
        FieldLabel("Mật khẩu")
        AuthField(password, { password = it }, "••••••••", KeyboardType.Password, ImeAction.Next, secret = true)
        FieldLabel("Xác nhận mật khẩu")
        AuthField(confirm, { confirm = it }, "••••••••", KeyboardType.Password, ImeAction.Done, secret = true)
        if (confirm.isNotEmpty() && !matches) {
            Text("Mật khẩu xác nhận chưa khớp.", color = Color(0xFFB3261E),
                style = MaterialTheme.typography.labelMedium)
        }
        Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rules.forEach { (text, ok) -> RuleRow(text, ok) }
        }
        AnimatedVisibility(error.isNotEmpty()) { MessageBox(error, true) }
        Button(
            onClick = { submit() },
            enabled = !busy && valid,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text(if (busy) "Đang tạo tài khoản…" else "Đăng ký", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
        }
        Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Text("Đã có tài khoản? ", color = Muted)
            Text(
                "Đăng nhập", color = Indigo, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !busy) { goLogin() }.padding(vertical = 8.dp)
            )
        }
    }
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
    Panel("Cấp học") { levels.chunked(2).forEach { Choices(it, level) { value -> level = value } } }
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

private val welcomePages = listOf(
    Triple("AI Study Mentor", "Hỏi bất cứ điều gì.\nHọc tốt hơn cùng AI.", 0),
    Triple("Giải bài từng bước", "Nhập đề hoặc chụp ảnh, Vilao sẽ giải thích rõ ràng theo đúng cấp học của bạn.", 1),
    Triple("Luyện tập mỗi ngày", "Làm trắc nghiệm theo môn, lưu bài cần ôn và theo dõi tiến độ học của bạn.", 2)
)

@Composable
private fun WelcomeScreen(start: () -> Unit) {
    val pagerState = rememberPagerState { welcomePages.size }
    val scope = rememberCoroutineScope()
    val last = pagerState.currentPage == welcomePages.lastIndex
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF423D9D), Color(0xFF5B52C4)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(pagerState, Modifier.weight(1f).fillMaxWidth()) { index ->
                val (title, text, art) = welcomePages[index]
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                        when (art) {
                            0 -> Image(
                                painterResource(R.drawable.study_ai),
                                contentDescription = "Trợ lý AI Study Mentor",
                                modifier = Modifier.fillMaxWidth(0.9f).height(220.dp),
                                contentScale = ContentScale.Fit
                            )
                            1 -> StepsArt()
                            else -> QuizArt()
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        title, color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text, color = Color.White,
                        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(welcomePages.size) { index ->
                    val active = pagerState.currentPage == index
                    val width by animateDpAsState(if (active) 26.dp else 10.dp, label = "dotWidth")
                    val color by animateColorAsState(
                        if (active) Color.White else Color(0x66FFFFFF), label = "dotColor"
                    )
                    Box(Modifier.height(10.dp).width(width).background(color, CircleShape))
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (last) start()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Indigo),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    if (last) "Bắt đầu" else "Tiếp tục",
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium
                )
            }
            Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                if (!last) TextButton(onClick = start) { Text("Bỏ qua", color = Color.White) }
            }
        }
    }
}

@Composable
private fun WelcomeCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(0.92f),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun StepsArt() {
    WelcomeCard {
        Text("Giải phương trình 2x + 3 = 7", fontWeight = FontWeight.Bold)
        Text("1. Trừ 3 ở hai vế: 2x = 4")
        Text("2. Chia hai vế cho 2: x = 2")
        Text("3. Thử lại: 2 × 2 + 3 = 7")
        Box(Modifier.background(Lilac, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text("Đáp án: x = 2 ✓", color = Indigo, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuizArt() {
    WelcomeCard {
        Text("Câu 1 · Toán · Dễ", color = Indigo, style = MaterialTheme.typography.labelLarge)
        Text("Nghiệm của 2x + 3 = 7 là?", fontWeight = FontWeight.Bold)
        listOf("A. x = 1" to false, "B. x = 2" to true, "C. x = 3" to false).forEach { (label, correct) ->
            Box(
                Modifier.fillMaxWidth()
                    .background(if (correct) Color(0xFFDDF5E6) else Color(0xFFF1F1F8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) { Text(if (correct) "$label  ✓" else label) }
        }
        Text("+10 XP", color = Indigo, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BottomNav(current: Screen, select: (Screen) -> Unit) {
    val selected = when (current) {
        Screen.Profile -> Screen.Progress
        Screen.Bookmarks -> Screen.History
        else -> current
    }
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            navItems.forEach { item ->
                val active = item.screen == selected
                val boxColor by animateColorAsState(if (active) Indigo else Color.Transparent, label = "navBox")
                Column(
                    Modifier.weight(1f).clickable { select(item.screen) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier.size(34.dp).background(boxColor, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            item.icon, contentDescription = item.label,
                            tint = if (active) Color.White else Muted, modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        item.label, fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Indigo else Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(name: String, email: String, level: Int, xp: Int, target: Int, progress: Float) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(104.dp).border(3.dp, Color(0xFFB9B4F5), CircleShape).padding(5.dp)
                .background(Lilac, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), color = Indigo, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(email, color = Muted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🔥 Lv. $level", fontWeight = FontWeight.Bold, color = Color(0xFFE67E22))
            Text(
                "%,d / %,d XP".format(xp, target),
                Modifier.background(Lilac, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
                color = Indigo, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(0.6f).height(6.dp).clip(CircleShape),
            color = Color(0xFFFFB300), trackColor = Lilac
        )
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(24.dp))
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted)
        }
        HorizontalDivider(color = Color(0xFFEAEAF4))
    }
}

@Composable
private fun SelectIndicator(selected: Boolean) {
    Box(
        Modifier.size(24.dp)
            .background(if (selected) Indigo else Color.Transparent, CircleShape)
            .border(1.5.dp, if (selected) Indigo else Color(0xFFC9CAD9), CircleShape),
        contentAlignment = Alignment.Center
    ) { if (selected) Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun SelectCard(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val fill by animateColorAsState(if (selected) Color(0xFFEFEBFF) else Color.White, label = "cardFill")
    val line by animateColorAsState(if (selected) Indigo else Color(0xFFE8E8F2), label = "cardLine")
    Box(
        modifier.fillMaxWidth()
            .background(fill, RoundedCornerShape(16.dp))
            .border(if (selected) 1.5.dp else 1.dp, line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun PreferencesWizard(
    initialLevel: String,
    initialSubjects: List<String>,
    initialStyle: String,
    save: (String, List<String>, String) -> Unit,
    exit: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var level by rememberSaveable { mutableStateOf(initialLevel) }
    var picked by rememberSaveable { mutableStateOf(ArrayList(initialSubjects)) }
    var style by rememberSaveable { mutableStateOf(initialStyle) }
    BackHandler(step in 1..2) { step -= 1 }
    Column(
        Modifier.fillMaxSize().background(Color.White).systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            "←", color = Ink, style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.clickable { if (step in 1..2) step -= 1 else exit() }.padding(8.dp)
        )
        AnimatedContent(
            step, Modifier.weight(1f).fillMaxWidth(), label = "wizard",
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }
        ) { current ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (current) {
                    0 -> {
                        WizardTitle("Chọn cấp học")
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            Triple("Tiểu học", "Lớp 1 – 5", "✏️"),
                            Triple("THCS", "Lớp 6 – 9", "🎒"),
                            Triple("THPT", "Lớp 10 – 12", "📚"),
                            Triple("Đại học", "Sinh viên", "🎓")
                        ).forEach { (name, detail, emoji) ->
                            SelectCard(level == name, { level = name }) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        Modifier.size(48.dp).background(Color(0xFFFFF1DC), RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) { Text(emoji, fontSize = 24.sp) }
                                    Column(Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold)
                                        Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    SelectIndicator(level == name)
                                }
                            }
                        }
                    }
                    1 -> {
                        WizardTitle("Chọn môn học quan tâm")
                        Text(
                            "(Bạn có thể chọn nhiều môn)", color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        subjects.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { subject ->
                                    val on = subject in picked
                                    Box(Modifier.weight(1f)) {
                                        SelectCard(
                                            on,
                                            {
                                                picked = ArrayList(if (on) picked - subject else picked + subject)
                                            },
                                            Modifier.heightIn(min = 64.dp)
                                        ) {
                                            Text(
                                                subject, Modifier.align(Alignment.Center),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (on) {
                                            Box(
                                                Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp)
                                                    .size(22.dp).background(Indigo, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) { Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        WizardTitle("Chọn kiểu giải thích")
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "Ngắn gọn" to "Trả lời súc tích, đi thẳng vào ý chính",
                            "Chi tiết" to "Giải thích đầy đủ khái niệm và cách làm",
                            "Từng bước" to "Hướng dẫn giải theo từng bước cụ thể"
                        ).forEach { (name, detail) ->
                            SelectCard(style == name, { style = name }) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    SelectIndicator(style == name)
                                    Column {
                                        Text(name, fontWeight = FontWeight.Bold)
                                        Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Spacer(Modifier.height(40.dp))
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(120.dp).background(Color(0xFF3DBE78), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text("✓", color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Bold) }
                            Text("✦", color = Color(0xFFFFB300), fontSize = 22.sp,
                                modifier = Modifier.align(Alignment.TopCenter).offset(x = 50.dp))
                            Text("★", color = Color(0xFFFFB300), fontSize = 30.sp,
                                modifier = Modifier.align(Alignment.TopEnd).offset(y = 20.dp))
                            Text("✦", color = Color(0xFF29B6F6), fontSize = 18.sp,
                                modifier = Modifier.align(Alignment.CenterStart).offset(x = 40.dp, y = (-20).dp))
                            Text("✦", color = Color(0xFFEC407A), fontSize = 16.sp,
                                modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp))
                            Text("●", color = Color(0xFFFF9800), fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.CenterStart).offset(x = 20.dp, y = 40.dp))
                            Text("✦", color = Color(0xFF29B6F6), fontSize = 16.sp,
                                modifier = Modifier.align(Alignment.BottomCenter).offset(x = 60.dp))
                        }
                        Text(
                            "Thiết lập hoàn tất!", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Bây giờ bạn có thể bắt đầu học tập cùng AI Study Mentor.",
                            Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Muted
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                when (step) {
                    0 -> step = 1
                    1 -> step = 2
                    2 -> {
                        save(level, picked.toList(), style)
                        step = 3
                    }
                    else -> exit()
                }
            },
            enabled = step != 1 || picked.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                when (step) { 0, 1 -> "Tiếp tục"; 2 -> "Hoàn tất"; else -> "Vào ứng dụng" },
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun WizardTitle(text: String) {
    Text(
        text, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink
    )
}