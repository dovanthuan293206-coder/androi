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
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun LoginForm(auth: FirebaseAuth, goRegister: () -> Unit) {
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
