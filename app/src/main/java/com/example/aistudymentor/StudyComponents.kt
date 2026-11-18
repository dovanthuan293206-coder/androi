package com.example.aistudymentor

import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudymentor.data.StudyEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

@Composable
internal fun EntryList(entries: List<StudyEntry>, empty: String, open: (StudyEntry) -> Unit) {
    if (entries.isEmpty()) Text(empty)
    entries.forEach { entry ->
        val kind = if (entry.kind == "quiz") "Luyện tập" else "Hỏi đáp"
        Panel("${entry.subject} · $kind") {
            TextButton(onClick = { open(entry) }) { Text(entry.question) }
        }
    }
}

@Composable
internal fun QuizResult(quiz: JSONObject, answer: Int, select: (Int) -> Unit) {
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
internal fun AiAction(label: String, enabled: Boolean = true, run: suspend () -> Unit) {
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
internal fun Heading(title: String, description: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun Hero(kicker: String, title: String, detail: String) {
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
internal fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
internal fun Action(label: String, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(label) }
}

@Composable
internal fun Choices(options: List<String>, selected: String, choose: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> FilterChip(option == selected, { choose(option) }, { Text(option) }) }
    }
}

@Composable
internal fun ShortcutRow(first: Screen, second: Screen, select: (Screen) -> Unit) {
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
internal fun Metric(value: String, label: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = Indigo)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun BottomNav(current: Screen, select: (Screen) -> Unit) {
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
                val boxColor by animateColorAsState(if (active) Color(0xFF514BC4) else Color.Transparent, label = "navBox")
                Column(
                    Modifier.weight(1f).pressScale { select(item.screen) },
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
internal fun ProfileHeader(name: String, email: String, level: Int, xp: Int, target: Int, progress: Float) {
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
internal fun ProfileRow(icon: ImageVector, label: String, onClick: () -> Unit) {
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
