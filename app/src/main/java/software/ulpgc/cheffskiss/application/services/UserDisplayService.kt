package software.ulpgc.cheffskiss.application.services

import software.ulpgc.cheffskiss.domain.port.input.UserReader
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseUserReader
import java.util.UUID

class UserDisplayService(
    private val userReader: UserReader = FirebaseUserReader(),
) {
    suspend fun displayNameFor(userId: UUID): String =
        userReader.getByUid(userId.toString())?.username?.value?.takeIf { it.isNotBlank() } ?: ""
}
