package com.example.aistudymentor

import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudymentor.data.StudyEntry
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ProgressDashboard(
    entries: List<StudyEntry>,
    answerCount: Int,
    quizDone: Int,
    correctQuizzes: Int,
    xp: Int
) {
    var view by rememberSaveable { mutableStateOf("Tổng quan") }
    val accuracy = if (quizDone == 0) 0 else correctQuizzes * 100 / quizDone
    val subjectCounts = subjects.map { it to entries.count { entry -> entry.subject == it } }
        .filter { it.second > 0 }.sortedByDescending { it.second }
    val maxCount = (subjectCounts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Tiến độ & Thành tích", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Theo dõi hành trình học tập của bạn", fontSize = 12.sp, color = Muted)
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFEDECF6)).padding(4.dp)) {
            listOf("Tổng quan", "Chi tiết").forEach { tab ->
                val selected = view == tab
                val bg by animateColorAsState(if (selected) Color.White else Color.Transparent, label = "progressTab")
                Surface(onClick = { view = tab }, color = bg, shape = RoundedCornerShape(11.dp), modifier = Modifier.weight(1f)) {
                    Text(tab, modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center,
                        color = if (selected) Indigo else Muted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
        AnimatedContent(targetState = view, label = "progressContent", transitionSpec = {
            fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 14 } togetherWith fadeOut(tween(150))
        }) { selectedView ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (selectedView == "Tổng quan") {
                    Panel("Thống kê học tập") {
                        Text("Dữ liệu được tính từ hoạt động gần đây", color = Muted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HomeMetric(answerCount.toString(), "Câu hỏi", Indigo, Modifier.weight(1f))
                            HomeMetric("$accuracy%", "Quiz đúng", Good, Modifier.weight(1f))
                            HomeMetric("+$xp", "XP", Color(0xFFE3A02D), Modifier.weight(1f))
                        }
                    }
                    Panel("Hoạt động theo môn") {
                        if (subjectCounts.isEmpty()) Text("Chưa có dữ liệu. Hãy bắt đầu hỏi AI hoặc luyện tập nhé!", color = Muted, fontSize = 12.sp)
                        subjectCounts.forEach { (subject, count) ->
                            ProgressSubjectRow(subject, count.toFloat() / maxCount, "${count} hoạt động")
                        }
                    }
                    Panel("Nhận xét của AI  ✦") {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Lilac), contentAlignment = Alignment.Center) { Text("✦", color = Indigo, fontSize = 18.sp) }
                            Text(
                                when {
                                    entries.isEmpty() -> "Hãy bắt đầu bằng một câu hỏi hoặc bài luyện tập. Mỗi hoạt động sẽ giúp bạn xây dựng tiến độ học tập."
                                    accuracy >= 80 -> "Bạn đang làm rất tốt! Hãy duy trì nhịp học và thử thêm các chủ đề mới để củng cố kiến thức."
                                    else -> "Bạn đã có ${entries.size} hoạt động học tập. Hãy xem lại các câu hỏi đã lưu và luyện tập đều đặn để cải thiện kết quả nhé."
                                }, color = Ink, fontSize = 12.sp, lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    Panel("Chi tiết hoạt động") {
                        ProgressSubjectRow("Hỏi đáp AI", answerCount / (entries.size.coerceAtLeast(1)).toFloat(), "$answerCount lượt")
                        ProgressSubjectRow("Luyện tập", quizDone / (entries.size.coerceAtLeast(1)).toFloat(), "$quizDone bài")
                        ProgressSubjectRow("Trả lời đúng", if (quizDone == 0) 0f else correctQuizzes / quizDone.toFloat(), "$correctQuizzes/$quizDone câu")
                    }
                    Panel("Thành tích") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF1D9)), contentAlignment = Alignment.Center) { Text("🏆", fontSize = 23.sp) }
                            Column {
                                Text("Cấp độ ${xp / 200 + 1}", fontWeight = FontWeight.Bold, color = Ink)
                                Text("$xp XP tích lũy · ${200 - xp % 200} XP đến cấp tiếp theo", fontSize = 11.sp, color = Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProgressSubjectRow(subject: String, fraction: Float, detail: String) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), animationSpec = tween(850, easing = FastOutSlowInEasing), label = "progressBar")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(subject, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(detail, color = Muted, fontSize = 10.sp)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFFEDECF5))) {
            Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(Color(0xFF514BC4), Color(0xFF8B75EE), Color(0xFFB18AF3)))))
        }
    }
}
