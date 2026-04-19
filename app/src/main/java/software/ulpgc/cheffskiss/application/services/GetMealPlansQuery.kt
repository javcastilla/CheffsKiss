package software.ulpgc.cheffskiss.application.services

import kotlinx.coroutines.flow.Flow
import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.domain.model.MealPlan
import java.util.UUID

class GetMealPlansQuery(private val port: MealPlanRepository) {
    operator fun invoke(userId: UUID): Flow<List<MealPlan>> =
        port.getMealPlans(userId)
}
