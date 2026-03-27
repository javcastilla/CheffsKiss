package software.ulpgc.cheffskiss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.Input
import software.ulpgc.cheffskiss.application.LoginInput
import software.ulpgc.cheffskiss.application.RegisterUserCommand
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseUserNameReader
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthenticantionViewModel : ViewModel() {

    private val firebaseService = FirebaseAuthenticationService()  // ← una sola instancia
    private val userNameReader  = FirebaseUserNameReader()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun resetState() { _uiState.value = AuthUiState.Idle }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                firebaseService.login(email, password)  // ← llama directamente al servicio
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
        else                           -> msg
    }
}
