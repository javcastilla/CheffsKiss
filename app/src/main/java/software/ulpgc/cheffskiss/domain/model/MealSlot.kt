package software.ulpgc.cheffskiss.domain.model

import software.ulpgc.cheffskiss.domain.model.vo.SlotTime
import java.util.UUID

data class MealSlot(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val startTime: SlotTime,
    val endTime: SlotTime,
    val colorIndex: Int = 0,
    val recipeId: UUID? = null
) {
    init {
        require(name.isNotBlank()) { "Slot name cannot be blank" }
        require(startTime < endTime) { "Start time must be before end time" }
    }

    fun overlapsWith(other: MealSlot): Boolean =
        startTime < other.endTime && other.startTime < endTime
}