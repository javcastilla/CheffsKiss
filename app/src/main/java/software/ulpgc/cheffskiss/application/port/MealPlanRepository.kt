package software.ulpgc.cheffskiss.application.port

import kotlinx.coroutines.flow.Flow
import software.ulpgc.cheffskiss.domain.model.MealPlan
import java.util.UUID

interface MealPlanRepository {
    suspend fun createMealPlan(mealPlan: MealPlan)
    suspend fun updateMealPlan(mealPlan: MealPlan)
    suspend fun deleteMealPlan(planId: UUID, userId: UUID)
    suspend fun setActivePlan(planId: UUID, userId: UUID)
    fun getMealPlans(userId: UUID): Flow<List<MealPlan>>
}
