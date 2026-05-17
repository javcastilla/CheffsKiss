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
import software.ulpgc.cheffskiss.ui.components.CKTextField
import software.ulpgc.cheffskiss.ui.components.CKHelperText
import software.ulpgc.cheffskiss.ui.components.CKPasswordStrength
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
    val usernameAvailable by viewModel.usernameAvailable.collectAsState()
    var usernameTouched by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

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
                        .background(Background)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ChefKiss",
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
                colors = CardDefaults.cardColors(containerColor = software.ulpgc.cheffskiss.ui.theme.Surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {

                    // Username
                    CKTextField(
                        label = "Username",
                        value = username,
                        onValueChange = {
                            username = it
                            usernameTouched = true
                            viewModel.checkUsernameAvailability(it) },
                        placeholder = "chef_gourmet",
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = Outline) },
                        helper = when {
                            !usernameTouched           -> null
                            username.length <= 3       -> { { CKHelperText("Username length should be at least 4", Color(0xFFBA1A1A)) } }
                            usernameAvailable == true  -> { { CKHelperText("Username is available", Tertiary) } }
                            usernameAvailable == false -> { { CKHelperText("Username already taken", Color(0xFFBA1A1A)) } }
                            else                       -> null
                        }                    )

                    // Email
                    CKTextField(
                        label = "Email Address",
                        value = email,
                        onValueChange = { email = it
                                        emailError= null},
                        placeholder = "hello@cheffskiss.com",
                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = Outline) },
                        keyboardType = KeyboardType.Email,
                        helper = when{
                            emailError != null -> { { CKHelperText(emailError!!) } }
                            email.isEmpty() -> {null}
                            !viewModel.isValidEmail(email) -> { { CKHelperText("Enter a valid email address") } }
                            else -> null
                        }
                    )

                    // Password
                    CKTextField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it
                                        passwordError=  null},
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
                        isError = passwordError != null,
                        helper = when {
                            passwordError != null -> { { CKHelperText(passwordError!!) } }   // ← primero el error
                            password.isNotEmpty() -> { { CKPasswordStrength(strength, strengthLabel, strengthColor) } }
                            else -> null
                        }
                    )

                    // Confirm Password
                    CKTextField(
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
                            { CKHelperText("Passwords do not match", Color(0xFFBA1A1A)) }
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
                    val isFormValid = uiState !is AuthUiState.Loading
                            && username.length > 3
                            && usernameAvailable == true
                            && email.isNotBlank()
                            && viewModel.isValidEmail(email)
                            && emailError == null
                            && password.isNotBlank()
                            && strength >= 2
                            && passwordError == null
                            && confirmPassword.isNotBlank()
                            && password == confirmPassword
                    Button(
                        onClick = {
                            if (email.isBlank()) emailError= "Email is required"
                            if (password.isBlank()) passwordError = "Password is required"
                            if (isFormValid) {
                                viewModel.register(email, password, username, null, "")
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(6.dp)
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        if (isFormValid) {
                                            listOf(Primary, Color(0xFF004D1C))
                                        } else {
                                            listOf(
                                                Primary.copy(alpha = 0.4f),
                                                Color(0xFF004D1C).copy(alpha = 0.4f)
                                            )
                                        },
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

