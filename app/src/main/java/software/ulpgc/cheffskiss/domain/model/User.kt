package software.ulpgc.cheffskiss.domain.model

import java.util.UUID

data class User(
    val id: UUID,
    val image: String,
    val description: String?,
    val username: Username
)
