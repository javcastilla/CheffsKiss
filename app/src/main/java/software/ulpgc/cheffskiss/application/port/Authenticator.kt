package software.ulpgc.cheffskiss.application.port

interface Authenticator {
    suspend fun login(email: String, password: String): Boolean
}