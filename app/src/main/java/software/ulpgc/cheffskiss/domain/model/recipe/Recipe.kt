package software.ulpgc.cheffskiss.domain.model.recipe

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.domain.model.Step
import software.ulpgc.cheffskiss.domain.model.user.User
import java.net.URI
import java.util.UUID
import kotlin.time.Duration

data class Recipe(
    val id: UUID,
    val version: Int = 0,
    val title: String,
    val duration: Duration,
    val tags: List<String> = emptyList(),
    val servings: Int,
    val image: URI? = null,
    val timestamp: Instant = Clock.System.now(),
    val recipeLines: List<RecipeLine> = emptyList(),
    val steps: List<Step> = emptyList(),
    val creator: User,
) {
    fun titled(title: String): Recipe = copy(title = title)
    fun with(duration: Duration): Recipe = copy(duration = duration)
    fun with(tags: List<String>): Recipe = copy(tags = tags.toList())
    fun with(servings: Int): Recipe = copy(servings = servings)
    fun with(image: URI): Recipe = copy(image = image)
    fun createdBy(user: User): Recipe = copy(creator = user)
}