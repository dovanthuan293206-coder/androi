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
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val welcomePages = listOf(
    Triple("AI Study Mentor", "Hỏi bất cứ điều gì.\nHọc tốt hơn cùng AI.", 0),
    Triple("Giải bài từng bước", "Nhập đề hoặc chụp ảnh, Vilao sẽ giải thích rõ ràng theo đúng cấp học của bạn.", 1),
    Triple("Luyện tập mỗi ngày", "Làm trắc nghiệm theo môn, lưu bài cần ôn và theo dõi tiến độ học của bạn.", 2)
)

@Composable
internal fun WelcomeScreen(start: () -> Unit) {
    val pagerState = rememberPagerState { welcomePages.size }
    val scope = rememberCoroutineScope()
    val last = pagerState.currentPage == welcomePages.lastIndex
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF423D9D)
    ) {
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
}

@Composable
internal fun WelcomeCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(0.92f),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
internal fun StepsArt() {
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
internal fun QuizArt() {
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
