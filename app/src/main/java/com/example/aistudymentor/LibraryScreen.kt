package com.example.aistudymentor

import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun LibraryScreen(
    entries: List<StudyEntry>,
    openEntry: (StudyEntry) -> Unit,
    toggleBookmark: (StudyEntry) -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Tất cả") }
    val filters = listOf("Tất cả", "Toán", "Vật lý", "Hóa học", "Văn học")
    val filtered = entries.filter { entry ->
        val matchesSearch = entry.question.contains(search.trim(), ignoreCase = true) ||
            entry.subject.contains(search.trim(), ignoreCase = true)
        val matchesFilter = filter == "Tất cả" || entry.subject.equals(filter, ignoreCase = true)
        matchesSearch && matchesFilter
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Kho học tập", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Lưu lại và tìm nhanh những nội dung đã học", fontSize = 12.sp, color = Muted)
            }
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Lilac), contentAlignment = Alignment.Center) {
                Text("▤", fontSize = 22.sp, color = Indigo)
            }
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm câu hỏi, chủ đề đã lưu...") },
            leadingIcon = { Text("⌕", color = Indigo, fontSize = 23.sp) },
            trailingIcon = {
                if (search.isNotEmpty()) TextButton(onClick = { search = "" }) { Text("Xóa") }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = FieldBorder,
                focusedBorderColor = Indigo
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            filters.forEach { item ->
                val selected = filter == item
                val fill by animateColorAsState(if (selected) Indigo else Color.White, label = "libraryFilter")
                Surface(
                    onClick = { filter = item },
                    shape = RoundedCornerShape(50),
                    color = fill,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Indigo else FieldBorder)
                ) {
                    Text(item, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (selected) Color.White else Muted, fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LibrarySummary(entries.size.toString(), "Nội dung", Color(0xFFEDEBFF), Indigo, Modifier.weight(1f))
            LibrarySummary(entries.count { it.bookmarked }.toString(), "Đã đánh dấu", Color(0xFFFFF2DE), Color(0xFFCE8A22), Modifier.weight(1f))
            LibrarySummary(entries.count { it.kind == "quiz" }.toString(), "Bài luyện", Color(0xFFE5F6EC), Good, Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nội dung học tập", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
            Text("${filtered.size} mục", fontSize = 11.sp, color = Muted)
        }
        if (filtered.isEmpty()) {
            Panel("Chưa tìm thấy nội dung") {
                Text(if (entries.isEmpty()) "Khi bạn hỏi AI hoặc hoàn thành bài luyện tập, nội dung sẽ xuất hiện tại đây." else "Thử đổi từ khóa hoặc bộ lọc để tìm nội dung khác.", color = Muted)
            }
        } else {
            filtered.forEachIndexed { index, entry ->
                val tint = when (entry.subject) {
                    "Toán" -> Color(0xFFECEAFF)
                    "Vật lý" -> Color(0xFFE4F3FF)
                    "Hóa học" -> Color(0xFFFFF0E4)
                    else -> Color(0xFFE6F6ED)
                }
                val symbol = when (entry.kind) { "quiz" -> "✎" else -> "▤" }
                AnimatedVisibility(visible = true, enter = fadeIn(tween(220 + index * 45)) + slideInVertically(tween(240 + index * 35)) { it / 10 }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().pressScale { openEntry(entry) },
                        shape = RoundedCornerShape(17.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(tint), contentAlignment = Alignment.Center) {
                                Text(symbol, fontSize = 20.sp, color = Indigo, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f).padding(start = 11.dp, end = 5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(entry.question, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(color = tint, shape = RoundedCornerShape(6.dp)) {
                                        Text(entry.subject, color = Indigo, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                                    }
                                    Text(if (entry.kind == "quiz") "Luyện tập" else "Hỏi đáp", color = Muted, fontSize = 10.sp)
                                }
                            }
                            IconButton(onClick = { toggleBookmark(entry) }) {
                                Text(if (entry.bookmarked) "▮" else "♧", color = if (entry.bookmarked) Color(0xFFE5A12A) else Muted, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LibrarySummary(value: String, label: String, fill: Color, accent: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = fill)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Ink, fontSize = 10.sp, maxLines = 1)
        }
    }
}
