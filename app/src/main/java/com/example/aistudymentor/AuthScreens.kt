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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun authError(error: Exception?): String = when ((error as? FirebaseAuthException)?.errorCode) {
    "ERROR_WEAK_PASSWORD" -> "Mật khẩu chưa đủ mạnh."
    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email này đã có tài khoản."
    "ERROR_INVALID_EMAIL" -> "Email chưa đúng định dạng."
    "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "Email hoặc mật khẩu chưa đúng."
    "ERROR_OPERATION_NOT_ALLOWED" -> "Hãy bật phương thức Email/Password trong Firebase Authentication."
    else -> "Không thể xác thực. Kiểm tra email, mật khẩu và kết nối mạng."
}

@Composable
internal fun LoginScreen(auth: FirebaseAuth) {
    var register by rememberSaveable { mutableStateOf(false) }
    BackHandler(register) { register = false }
    Box(Modifier.fillMaxSize().background(Color.White)) {
        AnimatedContent(
            register, label = "auth",
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }
        ) { isRegister ->
            if (isRegister) RegisterForm(auth) { register = false }
            else LoginForm(auth) { register = true }
        }
    }
}

@Composable
internal fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    leading: String? = null,
    secret: Boolean = false
) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Muted) },
        singleLine = true,
        leadingIcon = if (leading != null) { { Text(leading) } } else null,
        trailingIcon = if (secret) {
            { Text(if (show) "🙈" else "👁", Modifier.clickable { show = !show }.padding(12.dp)) }
        } else null,
        visualTransformation = if (secret && !show) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldFill, unfocusedContainerColor = FieldFill,
            focusedBorderColor = Indigo, unfocusedBorderColor = FieldBorder
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (secret) KeyboardType.Password else keyboardType, imeAction = imeAction
        )
    )
}

@Composable
internal fun FieldLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge, color = Ink)
}

@Composable
internal fun MessageBox(text: String, isError: Boolean) {
    Box(
        Modifier.fillMaxWidth()
            .background(if (isError) Color(0xFFFDECEC) else Lilac, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) { Text(text, color = if (isError) Color(0xFFB3261E) else Ink) }
}

@Composable
internal fun SocialButton(icon: String, iconColor: Color, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FieldBorder)
    ) {
        Text(icon, color = iconColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(10.dp))
        Text(label, color = Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun RuleRow(text: String, ok: Boolean) {
    val color by animateColorAsState(if (ok) Good else Color(0xFFB8B9CC), label = "rule")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(20.dp).border(1.5.dp, color, CircleShape), contentAlignment = Alignment.Center) {
            Text("✓", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(text, color = if (ok) Ink else Muted)
    }
}
