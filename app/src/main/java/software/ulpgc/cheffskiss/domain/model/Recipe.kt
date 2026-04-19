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
    val duration: Duration,
    val ingredients: List<String>,
    val steps: List<Step>,
    val tags: List<String>,
    val image: String,
    val servings: Int,
    val createdAt: Instant = Clock.System.now()
) {
    init {
        require(author.isNotBlank()) { "Author cannot be blank" }
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
        require(duration > Duration.ZERO) { "Duration must be positive" }
        require(ingredients.isNotEmpty()) { "Recipe must have at least one ingredient" }
        require(steps.isNotEmpty()) { "Recipe must have at least one step" }
        require(servings > 0) { "Servings must be greater than zero" }
    }
}