package software.ulpgc.cheffskiss.ui.screen
import software.ulpgc.cheffskiss.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import software.ulpgc.cheffskiss.ui.AuthUiState
import software.ulpgc.cheffskiss.ui.AuthenticantionViewModel
import software.ulpgc.cheffskiss.ui.theme.*

// Colores extra del diseño RegisterScreen
private val SurfaceContainerLow  = Color(0xFFEDF5E1)
private val SurfaceContainerHigh = Color(0xFFE2EBD7)
private val Tertiary             = Color(0xFF6B8E23)

@Composable
fun RegisterScreen(
    viewModel: AuthenticantionViewModel = viewModel(),
    onRegisterSuccess: () -> Unit,
    onGoToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var username        by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible  by remember { mutableStateOf(false) }

    // Lógica del indicador de fortaleza
    val strength = when {
        password.length >= 10 && password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() } -> 4
        password.length >= 8 && password.any { it.isDigit() } -> 3
        password.length >= 6 -> 2
        password.isNotEmpty() -> 1
        else -> 0
    }
    val strengthLabel = when (strength) {
        4    -> "Great" ; 3 -> "Good" ; 2 -> "Fair" ; 1 -> "Weak" ; else -> ""
    }
    val strengthColor = when (strength) {
        4    -> Primary ; 3 -> Tertiary ; 2 -> Color(0xFFDAA520) ; else -> Color(0xFFBA1A1A)
    }

    val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetState()
            onRegisterSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Branding Header ──────────────────────────────
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Primary, Color(0xFF004D1C)),
                            start = Offset.Zero,
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cheffkiss_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Primary),
                    modifier = Modifier
                        .size(400.dp)
                        .rotate(-2f)
                        .shadow(14.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Background) // ← fondo verde oscuro detrás del pato
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CheffsKiss",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = OnSurface,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Join our digital heirloom kitchen",
                style = MaterialTheme.typography.bodyMedium,
                color = CKOnSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Form Card ────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {

                    // Username
                    RegisterField(
                        label = "Username",
                        value = username,
                        onValueChange = { username = it },
                        placeholder = "chef_gourmet",
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = Outline) },
                        helper = if (username.length >= 3) {
                            { HelperText("✓  Username is available", Tertiary) }
                        } else null
                    )

                    // Email
                    RegisterField(
                        label = "Email Address",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "hello@cheffskiss.com",
                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = Outline) },
                        keyboardType = KeyboardType.Email
                    )

                    // Password
                    RegisterField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••",
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Outline) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    null, tint = Outline
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        helper = if (password.isNotEmpty()) {
                            { PasswordStrengthIndicator(strength, strengthLabel, strengthColor) }
                        } else null
                    )

                    // Confirm Password
                    RegisterField(
                        label = "Confirm Password",
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "••••••••",
                        leadingIcon = { Icon(Icons.Filled.LockReset, null, tint = Outline) },
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(
                                    if (confirmVisible) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    null, tint = Outline
                                )
                            }
                        },
                        visualTransformation = if (confirmVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        isError = !passwordsMatch,
                        helper = if (!passwordsMatch) {
                            { HelperText("Passwords do not match", Color(0xFFBA1A1A)) }
                        } else null
                    )

                    // Error general
                    if (uiState is AuthUiState.Error) {
                        Text(
                            text = (uiState as AuthUiState.Error).message,
                            color = Color(0xFFBA1A1A),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Botón Create Account
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            if (passwordsMatch && password == confirmPassword) {
                                viewModel.register(email, password, username, null, "")
                            }
                        },
                        enabled = uiState !is AuthUiState.Loading
                                && username.isNotBlank()
                                && email.isNotBlank()
                                && password.isNotBlank()
                                && passwordsMatch,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(50.dp),  // rounded-full
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Primary, Color(0xFF004D1C)),
                                        start = Offset.Zero,
                                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                                    ),
                                    RoundedCornerShape(50.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Create Account", color = Color.White,
                                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer link
            Row {
                Text("Already have an account?",
                    color = CKOnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Log in",
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onGoToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ── Componentes auxiliares ───────────────────────────────────

@Composable
private fun RegisterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    helper: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = CKOnSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Outline.copy(alpha = 0.5f)) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = keyboardType
            ),
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary.copy(alpha = 0.4f),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = SurfaceContainerLow,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                cursorColor = Primary,
                errorBorderColor = Color(0xFFBA1A1A)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        helper?.invoke()
    }
}

@Composable
private fun HelperText(text: String, color: Color) {
    Text(text,
        fontSize = 11.sp,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun PasswordStrengthIndicator(strength: Int, label: String, color: Color) {
    Column(
        modifier = Modifier.padding(start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (index < strength) color else SurfaceContainerHigh,
                            RoundedCornerShape(50.dp)
                        )
                )
            }
        }
        if (label.isNotEmpty()) {
            Text(
                text = "Strength: ",
                fontSize = 11.sp,
                color = CKOnSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}