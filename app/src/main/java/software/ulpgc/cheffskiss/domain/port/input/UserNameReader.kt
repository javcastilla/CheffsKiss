package software.ulpgc.cheffskiss.domain.port.input

import software.ulpgc.cheffskiss.domain.model.UserName

interface UserNameReader {
    suspend fun exist(value: UserName): Boolean
    suspend fun getUsernameByUid(uid: String): String?
}