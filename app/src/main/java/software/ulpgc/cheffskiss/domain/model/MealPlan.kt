package software.ulpgc.cheffskiss.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

data class MealPlan(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val name: String,
    val isActive: Boolean = false,
    val createdAt: Instant = Clock.System.now(),
    val days: Map<Weekday, List<MealSlot>> = Weekday.entries.associateWith { emptyList() }
)
