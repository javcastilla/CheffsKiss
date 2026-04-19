package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.control.Input
import software.ulpgc.cheffskiss.application.control.LogoutUserCommand
import software.ulpgc.cheffskiss.application.control.RegisterUserCommand
import software.ulpgc.cheffskiss.domain.model.UserName
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseUserNameReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthenticantionViewModel : ViewModel() {

    private val firebaseService = FirebaseAuthenticationService()
    private val userNameReader  = FirebaseUserNameReader()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun resetState() { _uiState.value = AuthUiState.Idle }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                firebaseService.login(email, password)
            }.fold(
                onSuccess = { ok ->
                    _uiState.value = if (ok) AuthUiState.Success
                    else AuthUiState.Error("Invalid email or password")
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState.Error(friendlyError(e.message))
                }
            )
        }
    }

    fun register(email: String, password: String, username: String,
                 description: String?, image: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val command = RegisterUserCommand(
                userNameReader = userNameReader,
                userPort = firebaseService,
                registerUserInput = object : Input {
                    override fun email()       = email
                    override fun password()    = password
                    override fun username()    = username
                    override fun description() = description
                    override fun image()       = image
                }
            )
            runCatching { command.execute() }
                .fold(
                    onSuccess = { _uiState.value = AuthUiState.Success },
                    onFailure = { e -> _uiState.value = AuthUiState.Error(e.message ?: "Unknown error") }
                )
        }
    }
    fun logout() {
        viewModelScope.launch {
            val command = LogoutUserCommand(firebaseService)
            runCatching { command.execute() }
                .fold(
                    onSuccess = { _uiState.value = AuthUiState.Success },
                    onFailure = { e -> _uiState.value = AuthUiState.Error(e.message ?: "Unknown error") }
                )
        }
    }
    fun getCurrentUid(): String? = firebaseService.getCurrentUser()
    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    val usernameAvailable: StateFlow<Boolean?> = _usernameAvailable.asStateFlow()

    fun checkUsernameAvailability(username: String) {
        if (username.length <= 3) {
            _usernameAvailable.value = null
            return
        }
        viewModelScope.launch {
            val exists = userNameReader.exist(toUserName(username))
            _usernameAvailable.value = !exists
        }
    }
     fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    private fun toUserName(username: String): UserName { return UserName(username) }

    private fun friendlyError(msg: String?) = when {
        msg == null                    -> "Unknown error"
        "EMAIL_ALREADY_IN_USE" in msg  -> "This email is already registered"
        "WEAK_PASSWORD"        in msg  -> "Password must be at least 6 characters"
        "INVALID_EMAIL"        in msg  -> "Invalid email format"
        "NETWORK"              in msg  -> "No internet connection"
        "offline"              in msg  -> "No internet connection"
        "client is offline"    in msg  -> "No internet connection"
        "USERNAME_ALREADY"     in msg  -> "This username is already taken"
        "Username already"     in msg  -> "This username is already taken"
        "INVALID_EMAIL" in msg -> "The email address is not valid"
        "EMAIL_NOT_FOUND" in msg ||
                "NO_SUCH_USER" in msg ||
                "USER_NOT_FOUND" in msg   -> "No account found with this email"
        "INVALID_PASSWORD" in msg ||
                "WRONG_PASSWORD" in msg ||
                "INVALID_LOGIN_CREDENTIALS" in msg -> "Incorrect password"
        "USER_DISABLED" in msg    -> "This account has been disabled"
        "TOO_MANY_ATTEMPTS" in msg ||
                "TOO_MANY_REQUESTS" in msg -> "Too many failed attempts. Try again later"
        "NETWORK_ERROR" in msg ||
                "TIMEOUT" in msg          -> "Network error. Check your connection"
        else                           -> msg
    }
}
