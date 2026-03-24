package software.ulpgc.cheffskiss.application.port.output

interface LoginPort {
    suspend fun login(email: String, password: String): Boolean
}