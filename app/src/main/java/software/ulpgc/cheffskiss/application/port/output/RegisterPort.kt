package software.ulpgc.cheffskiss.application.port.output

import java.util.UUID

interface RegisterPort {
    suspend fun register(email: String, password: String, username: String, description: String?, image: String): UUID?
}