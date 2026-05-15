package software.ulpgc.cheffskiss.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.domain.enum.MealPlanStatus
import java.util.UUID

data class MealPlanVersion(
    val id: UUID = UUID.randomUUID(),
    val timestamp: Instant = Clock.System.now(),
    val mealPlan: MealPlan,
    val status: MealPlanStatus,
) {
    fun update(mealPlan: MealPlan): MealPlanVersion = copy(mealPlan = mealPlan, timestamp = Clock.System.now())
    fun withStatus(status: MealPlanStatus): MealPlanVersion = copy(status = status)
}