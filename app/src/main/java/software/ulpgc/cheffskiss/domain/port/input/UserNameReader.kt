package software.ulpgc.cheffskiss.domain.port.input

import software.ulpgc.cheffskiss.domain.vo.Username

interface UserNameReader {
    suspend fun exist(value: Username): Boolean
    suspend fun getUsernameByUid(uid: String): String?
}