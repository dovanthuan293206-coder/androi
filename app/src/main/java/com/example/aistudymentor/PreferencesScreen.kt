package com.example.aistudymentor

import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SelectIndicator(selected: Boolean) {
    Box(
        Modifier.size(24.dp)
            .background(if (selected) Indigo else Color.Transparent, CircleShape)
            .border(1.5.dp, if (selected) Indigo else Color(0xFFC9CAD9), CircleShape),
        contentAlignment = Alignment.Center
    ) { if (selected) Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@Composable
internal fun SelectCard(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
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
internal fun PreferencesWizard(
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
internal fun WizardTitle(text: String) {
    Text(
        text, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink
    )
}
