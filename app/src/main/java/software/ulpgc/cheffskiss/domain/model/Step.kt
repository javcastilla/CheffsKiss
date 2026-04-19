package software.ulpgc.cheffskiss.domain.model

import java.util.UUID
import kotlin.time.Duration

data class Step(
    val id: UUID = UUID.randomUUID(),
    val description: String,
    val duration: Duration,
    val cardinal: Int,
    val image: String = ""
) {
    init {
        require(description.isNotBlank()) { "Step description cannot be blank" }
        require(cardinal > 0) { "Cardinal must be greater than zero" }
    }
}