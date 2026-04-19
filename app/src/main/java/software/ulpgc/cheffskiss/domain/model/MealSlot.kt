package software.ulpgc.cheffskiss.domain.model

import software.ulpgc.cheffskiss.domain.model.vo.SlotTime
import java.util.UUID

data class MealSlot(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val startTime: SlotTime,
    val endTime: SlotTime,
    val recipeId: UUID? = null,
    val colorIndex: Int = 0
) {
    init {
        require(name.isNotBlank())
        require(endTime > startTime)
    }

    fun overlapsWith(other: MealSlot): Boolean =
        this.startTime < other.endTime && other.startTime < this.endTime
}