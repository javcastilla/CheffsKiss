package software.ulpgc.cheffskiss.domain.model

import java.util.UUID
import kotlin.time.Duration

data class Step(
    val id: UUID,
    val description: String,
    val duration: Duration? = null,
    val cardinal: Int,
    val imageUrl: String? = null,
) {
    fun with(description: String): Step = copy(description = description)
    fun with(duration: Duration): Step = copy(duration = duration)
    fun with(cardinal: Int): Step = copy(cardinal = cardinal)
    fun withImageUrl(imageUrl: String?): Step = copy(imageUrl = imageUrl)

    fun hasTimer(): Boolean = duration != null && duration.inWholeSeconds > 0

    fun hasMedia(): Boolean = !imageUrl.isNullOrBlank()

    fun timerSeconds(): Long = duration?.inWholeSeconds ?: 0L
}