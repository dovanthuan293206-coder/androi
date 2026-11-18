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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun RegisterForm(auth: FirebaseAuth, goLogin: () -> Unit) {
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
