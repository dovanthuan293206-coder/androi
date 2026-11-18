package com.example.aistudymentor

import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
internal fun HomeDashboard(
    name: String,
    level: Int,
    questionCount: Int,
    accuracy: Int,
    xp: Int,
    recentEntry: StudyEntry?,
    onAsk: (String) -> Unit,
    onQuiz: () -> Unit,
    onLibrary: () -> Unit,
    onOpenEntry: (StudyEntry) -> Unit,
    onProfile: () -> Unit
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(360)) + slideInVertically(tween(360)) { it / 12 }
    ) {
    Column(verticalArrangement = Arrangement.spacedBy(17.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF514BC4), Color(0xFF7770E8), Color(0xFF9B83ED))))
                .padding(horizontal = 17.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Xin chào, $name! 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Sẵn sàng học hôm nay?", fontSize = 13.sp, color = Color(0xFFE9E7FF))
            }
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f), modifier = Modifier.pressScale(onProfile)) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Text("Lv. $level", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Hỏi bất cứ điều gì...") },
            leadingIcon = { Text("✦", color = Indigo, fontSize = 20.sp) },
            trailingIcon = {
                IconButton(onClick = { onAsk(searchText) }) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Hỏi AI", tint = Indigo)
                }
            },
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color(0xFFDCD9F6),
                focusedBorderColor = Color(0xFF6A63DE), cursorColor = Indigo
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeShortcut("Hỏi AI", "✦", Color(0xFFF0EDFF), Modifier.weight(1f)) { onAsk(searchText) }
            HomeShortcut("Luyện tập", "✎", Color(0xFFFFF1E5), Modifier.weight(1f), onQuiz)
            HomeShortcut("Thư viện", "▤", Color(0xFFE6F7EF), Modifier.weight(1f), onLibrary)
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Ôn tập hôm nay", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Đề xuất dựa trên hoạt động của bạn", fontSize = 12.sp, color = Muted)
            }
            Card(
                modifier = Modifier.fillMaxWidth().pressScale { if (recentEntry != null) onOpenEntry(recentEntry) else onQuiz() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFE4F6EC)), contentAlignment = Alignment.Center) {
                        Text("✓", color = Color(0xFF269B62), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f).padding(start = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(recentEntry?.subject ?: "Bắt đầu luyện tập", fontWeight = FontWeight.Bold, color = Ink)
                        Text(recentEntry?.question ?: "Thử một bài luyện tập ngắn nhé", color = Muted, fontSize = 12.sp, maxLines = 1)
                    }
                    Text("›", color = Indigo, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Thống kê nhanh", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                HomeMetric(questionCount.toString(), "Câu hỏi", Color(0xFF514DB7), Modifier.weight(1f))
                HomeMetric("$accuracy%", "Quiz đúng", Color(0xFF269B62), Modifier.weight(1f))
                HomeMetric("+$xp", "XP tích lũy", Color(0xFFE59A26), Modifier.weight(1f))
            }
        }
    }
    }
}

@Composable
internal fun HomeShortcut(label: String, symbol: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(92.dp).pressScale(onClick),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 7.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(tint), contentAlignment = Alignment.Center) {
                Text(symbol, fontSize = 21.sp, color = when (label) { "Luyện tập" -> Color(0xFFEF8A2F); "Thư viện" -> Color(0xFF159B70); else -> Indigo }, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
        }
    }
}

@Composable
internal fun HomeMetric(value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    val tint = when (valueColor) {
        Good, Color(0xFF269B62) -> Color(0xFFE3F7ED)
        Color(0xFFE59A26), Color(0xFFE3A02D) -> Color(0xFFFFF1DB)
        else -> Color(0xFFECEBFF)
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = tint),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(label, fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

/** Co nhẹ khi nhấn, kết hợp ripple mặc định của Material để thao tác có phản hồi rõ ràng. */
internal fun Modifier.pressScale(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = 115, easing = FastOutSlowInEasing),
        label = "pressScale"
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
}
