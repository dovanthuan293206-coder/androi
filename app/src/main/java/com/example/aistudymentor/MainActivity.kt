package com.example.aistudymentor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.Icons
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudymentor.data.StudyAi
import com.example.aistudymentor.data.StudyCloud
import com.example.aistudymentor.data.StudyEntry
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal val Indigo = Color(0xFF4543AB)
internal val Ink = Color(0xFF171C38)
internal val Canvas = Color(0xFFF6F7FC)
internal val Lilac = Color(0xFFE9E8FF)
internal val subjects = listOf("Toán", "Vật lý", "Hóa học", "Sinh học", "Lập trình", "Lịch sử", "Ngôn ngữ", "Khác")
internal val levels = listOf("Tiểu học", "THCS", "THPT", "Đại học")
internal val styles = listOf("Ngắn gọn", "Chi tiết", "Từng bước")
internal const val Example = "Giải phương trình 2x + 3 = 7"

internal enum class Screen(val title: String, val icon: String = "") {
    Login("Đăng nhập"), Preferences("Thiết lập học tập"),
    Home("Trang chủ", "⌂"), Question("Đặt câu hỏi"), Answer("Lời giải"),
    History("Lịch sử", "◷"), Bookmarks("Đã lưu"), Quiz("Luyện tập"),
    Progress("Tiến độ", "▥"), Profile("Cá nhân", "◎"),
    Achievements("Thành tích & Huy hiệu"), Settings("Cài đặt"), Help("Trợ giúp & Phản hồi")
}

internal val tabs = listOf(Screen.Home, Screen.Question, Screen.Quiz, Screen.History, Screen.Progress)

internal data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

internal val navItems = listOf(
    NavItem(Screen.Home, "Trang chủ", Icons.Outlined.Home),
    NavItem(Screen.Question, "Hỏi AI", Icons.Outlined.ChatBubbleOutline),
    NavItem(Screen.Quiz, "Luyện tập", Icons.Outlined.Edit),
    NavItem(Screen.History, "Thư viện", Icons.Outlined.Book),
    NavItem(Screen.Progress, "Tiến độ", Icons.Outlined.BarChart)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Indigo, onPrimary = Color.White,
                    primaryContainer = Lilac, onPrimaryContainer = Ink,
                    background = Canvas, onBackground = Ink, surface = Color.White,
                    onSurface = Ink, secondary = Color(0xFF16A879),
                    tertiary = Color(0xFFFFA33B),
                    surfaceVariant = Color(0xFFF0EEFF), onSurfaceVariant = Muted,
                    secondaryContainer = Color(0xFFDDF7EC), tertiaryContainer = Color(0xFFFFEBD5)
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
            if (screen != Screen.Home) CenterAlignedTopAppBar(
                title = { Text(screen.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (screen != Screen.Home) {
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
            val scroll = rememberScrollState()
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(scroll)
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
                        val displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Bạn" }
                        val correctQuizzes = entries.count { entry ->
                            if (entry.kind != "quiz" || entry.selection == null) false
                            else runCatching { JSONObject(entry.content).optInt("correctIndex", -1) == entry.selection }.getOrDefault(false)
                        }
                        val accuracy = if (quizDone == 0) 0 else (correctQuizzes * 100 / quizDone)
                        HomeDashboard(
                            name = displayName,
                            level = levelNo,
                            questionCount = answerCount,
                            accuracy = accuracy,
                            xp = xp,
                            recentEntry = entries.firstOrNull(),
                            onAsk = { prompt -> question = prompt; open(Screen.Question) },
                            onQuiz = { open(Screen.Quiz) },
                            onLibrary = { open(Screen.History) },
                            onOpenEntry = { showEntry(it) },
                            onProfile = { open(Screen.Profile) }
                        )
                    }
                    Screen.Question -> QuestionPanel(
                        question = question,
                        updateQuestion = { question = it },
                        preferredLevel = level,
                        style = style,
                        apiKey = vilaoKey,
                        scroll = scroll,
                        showAnswer = { prompt, answer, subject ->
                            activeEntryId = cloud.add("answer", prompt, answer, subject, writeFailed)
                            answerQuestion = prompt
                            aiAnswer = answer
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
                            AnswerCard(aiAnswer, generateImage = { StudyAi.illustrate(vilaoKey, aiAnswer) })
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
                    Screen.History -> LibraryScreen(
                        entries = entries,
                        openEntry = ::showEntry,
                        toggleBookmark = { entry -> cloud.bookmark(entry.id, !entry.bookmarked, writeFailed) }
                    )
                    Screen.Bookmarks -> {
                        EntryList(entries.filter { it.bookmarked }, "Chưa có bài đã lưu", ::showEntry)
                    }
                    Screen.Quiz -> QuizPanel(level, vilaoKey, scroll, create = { subject, quiz ->
                        cloud.add("quiz", JSONObject(quiz).getString("question"), quiz, subject, writeFailed)
                    }, select = { id, choice -> cloud.select(id, choice, writeFailed) })
                    Screen.Progress -> ProgressDashboard(
                        entries = entries,
                        answerCount = answerCount,
                        quizDone = quizDone,
                        correctQuizzes = entries.count { entry ->
                            if (entry.kind != "quiz" || entry.selection == null) false
                            else runCatching { JSONObject(entry.content).optInt("correctIndex", -1) == entry.selection }.getOrDefault(false)
                        },
                        xp = xp
                    )
                    Screen.Profile -> ProfileScreen(email, levelNo, xp, level, ::open, signOut)
                    Screen.Achievements -> AchievementsScreen(answerCount, quizDone, entries, levelNo)
                    Screen.Settings -> SettingsScreen(vilaoKey, { vilaoKey = it }, level, selectedSubjects, style, email, ::open)
                    Screen.Help -> HelpScreen()
                }
            }
        }
    }
}

internal val FieldFill = Color(0xFFF3F2FB)
internal val FieldBorder = Color(0xFFE2E1F2)
internal val Muted = Color(0xFF7B7D98)
internal val Good = Color(0xFF2E9E5B)
