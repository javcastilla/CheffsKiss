package software.ulpgc.cheffskiss.domain.port.input

import software.ulpgc.cheffskiss.domain.model.Username

interface UserNameReader {
    suspend fun exist(value: Username): Boolean
}