package software.ulpgc.cheffskiss.domain.model

import software.ulpgc.cheffskiss.domain.vo.Description
import software.ulpgc.cheffskiss.domain.vo.Username
import java.net.URI
import java.util.UUID

data class User(
    val id: UUID,
    val username: Username? = null,
    val image: URI? = null,
    val description: Description? = null,
) {
    fun withUsername(username: Username): User = copy(username = username)
    fun with(image: URI): User = copy(image = image)
    fun with(description: Description): User = copy(description = description)
}
