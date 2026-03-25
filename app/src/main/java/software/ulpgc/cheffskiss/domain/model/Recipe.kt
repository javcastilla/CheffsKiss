package software.ulpgc.cheffskiss.domain.model

import java.util.UUID
import kotlin.time.Duration

data class Recipe(
    val id: UUID,
    val title: String,
    val duration: Duration,
    val tags: List<String>,
    val image: String,
    val user: User
) {
}
