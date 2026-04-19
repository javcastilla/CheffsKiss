package software.ulpgc.cheffskiss.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.domain.model.vo.Weekday
import java.util.UUID

data class MealPlan(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val name: String,
    val isActive: Boolean = false,
    val createdAt: Instant = Clock.System.now(),
    val days: Map<Weekday, List<MealSlot>> = Weekday.entries.associateWith { emptyList() }
) {
    fun addSlot(day: Weekday, slot: MealSlot): MealPlan {
        val current = days[day] ?: emptyList()
        require(current.none { it.overlapsWith(slot) }) {
            "Slot $slot overlaps with an existing slot on $day"
        }
        return copy(days = days + (day to (current + slot).sortedBy { it.startTime }))
    }

    fun updateSlot(day: Weekday, slot: MealSlot): MealPlan {
        val others = (days[day] ?: emptyList()).filter { it.id != slot.id }
        require(others.none { it.overlapsWith(slot) }) {
            "Updated slot $slot overlaps with an existing slot on $day"
        }
        return copy(days = days + (day to (others + slot).sortedBy { it.startTime }))
    }

    fun removeSlot(day: Weekday, slotId: UUID): MealPlan {
        val updated = (days[day] ?: emptyList()).filter { it.id != slotId }
        return copy(days = days + (day to updated))
    }

    fun activate(): MealPlan = copy(isActive = true)

    fun deactivate(): MealPlan = copy(isActive = false)
}