package software.ulpgc.cheffskiss.domain.model

import java.util.UUID
import kotlin.time.Duration

data class Recipe(
    val id: UUID,
    val author: String,
    val title: String,
    val description: String,
    val duration: Duration,
    val ingredients: List<String>,
    val steps: List<Step>,
    val tags: List<String>,
    val image: String,
    val servings: Int,
)
