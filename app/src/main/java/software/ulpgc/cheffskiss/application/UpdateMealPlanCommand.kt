package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.output.MealPlanPort
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.MealPlan

class UpdateMealPlanCommand(
    private val port: MealPlanPort,
    private val mealPlan: MealPlan
) : Command {
    override suspend fun execute() {
        port.updateMealPlan(mealPlan)
    }
}
