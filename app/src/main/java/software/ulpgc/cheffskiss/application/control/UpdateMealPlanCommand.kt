package software.ulpgc.cheffskiss.application.control

import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlan

class UpdateMealPlanCommand(
    private val port: MealPlanRepository,
    private val mealPlan: MealPlan
) : Command {
    override suspend fun execute() {
        port.updateMealPlan(mealPlan)
    }
}
