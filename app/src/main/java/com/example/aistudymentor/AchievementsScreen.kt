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

@Composable
internal fun AchievementsScreen(answerCount: Int, quizDone: Int, entries: List<StudyEntry>, levelNo: Int) {
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
