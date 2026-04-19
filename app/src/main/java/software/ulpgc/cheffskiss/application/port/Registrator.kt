package software.ulpgc.cheffskiss.application.port

import java.util.UUID

interface Registrator {
    suspend fun register(email: String, password: String, username: String, description: String?, image: String?): UUID?
}