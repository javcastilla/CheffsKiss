package software.ulpgc.cheffskiss.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
import kotlin.time.Duration

data class Recipe(
    val id: UUID = UUID.randomUUID(),
    val author: String,
    val title: String,
    val description: String,
    val servings: Int,
    val duration: Duration,
    val tags: List<String> = emptyList(),
    val image: String = "",
    val createdAt: Instant = Clock.System.now()
) {
    init {
        require(title.isNotBlank())       { "Title cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
        require(servings > 0)             { "Servings must be greater than zero" }
        require(duration.isPositive())    { "Duration must be positive" }
    }
}