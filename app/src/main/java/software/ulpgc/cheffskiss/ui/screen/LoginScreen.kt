package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import software.ulpgc.cheffskiss.R
import software.ulpgc.cheffskiss.ui.AuthUiState
import software.ulpgc.cheffskiss.ui.AuthenticantionViewModel
import software.ulpgc.cheffskiss.ui.components.CKHelperText
import software.ulpgc.cheffskiss.ui.components.CKTextField
import software.ulpgc.cheffskiss.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: AuthenticantionViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                viewModel.resetState()
                onLoginSuccess()
            }
            is AuthUiState.Error -> {
                val msg = (uiState as AuthUiState.Error).message
                when {
                    "email" in msg.lowercase() || "found" in msg.lowercase() || "account" in msg.lowercase() -> {
                        emailError = msg
                        passwordError = null
                    }
                    "password" in msg.lowercase() || "credential" in msg.lowercase() || "incorrect" in msg.lowercase() -> {
                        passwordError = msg
                        emailError = null
                    }
                    "disabled" in msg.lowercase() -> {
                        emailError = msg
                        passwordError = null
                    }
                    "attempts" in msg.lowercase() || "requests" in msg.lowercase() -> {
                        passwordError = msg
                        emailError = null
                    }
                    else -> {
                        passwordError = msg
                        emailError = null
                    }
                }
            }
            else -> {}
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        // Glow top-right
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 100.dp, y = (-80).dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(listOf(CKSecondary.copy(alpha = 0.2f), Color.Transparent)),
                    CircleShape
                )
                .blur(60.dp)
        )
        // Glow bottom-left
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = 80.dp)
                .align(Alignment.BottomStart)
                .background(
                    Brush.radialGradient(listOf(Primary.copy(alpha = 0.1f), Color.Transparent)),
                    CircleShape
                )
                .blur(60.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // ── Logo ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .rotate(-2f)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .background(Primary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cheffkiss_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Primary),
                    modifier = Modifier
                        .size(400.dp)
                        .rotate(-2f)
                        .shadow(14.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Background)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Cheffs Kiss",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = Primary,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Reclaiming the warmth of\nyour digital kitchen",
                style = MaterialTheme.typography.bodyMedium,
                color = CKOnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Card ─────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)  // igual que RegisterScreen
                ) {


                    CKTextField(
                        label = "Email",
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
                        placeholder = "chef@cheffskiss.com",
                        leadingIcon = { Icon(Icons.Default.Mail, null, tint = Outline) },
                        keyboardType = KeyboardType.Email,
                        isError = emailError != null,
                        helper = when {
                            emailError != null -> { { CKHelperText(emailError!!) } }
                            email.isNotEmpty() && !viewModel.isValidEmail(email) ->
                            { { CKHelperText("Enter a valid email address") } }
                            else -> null
                        }
                    )

                    CKTextField(
                        label = "Password",
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null},
                        placeholder = "••••••••",
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Outline) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    null,
                                    tint = Primary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError != null,
                        helper = if (passwordError != null) {
                            { CKHelperText(passwordError!!) }
                        } else null
                    )




                    Button(
                        onClick =  {
                            if (email.isBlank()) emailError = "Email is required"
                            if (password.isBlank()) passwordError = "Password is required"

                            if (emailError == null && passwordError == null) {
                                viewModel.login(email, password)
                            }
                        },
                        enabled = uiState !is AuthUiState.Loading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        if (uiState !is AuthUiState.Loading && email.isNotBlank() && password.isNotBlank()){
                                            listOf(Color(0xFF003314), Color(0xFF004D1F))
                                        }else{
                                            listOf(
                                                Primary.copy(alpha = 0.4f),
                                                Color(0xFF003314).copy(alpha = 0.4f)
                                            )
                                        }
                                        ,
                                        start = Offset.Zero,
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    ),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(
                                    color = OnPrimary,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Log In",
                                    color = OnPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Forgot password?",
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row {
                Text(
                    "Don't have an account?",
                    color = CKOnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Sign up",
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable { onGoToRegister() }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(8.dp))

            // ── Footer accent bar ─────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight().background(Primary))
                Box(Modifier.weight(1f).fillMaxHeight().background(CKSecondary))
                Box(Modifier.weight(1f).fillMaxHeight().background(Outline))
                Box(Modifier.weight(1f).fillMaxHeight().background(Primary.copy(alpha = 0.8f)))
            }
        }
    }
}
