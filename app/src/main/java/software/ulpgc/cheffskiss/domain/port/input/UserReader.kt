package software.ulpgc.cheffskiss.domain.port.input

import software.ulpgc.cheffskiss.domain.model.user.User

interface UserReader {
    suspend fun getByEmail(email: String): User?
    suspend fun getByUid(uid: String): User?
}




