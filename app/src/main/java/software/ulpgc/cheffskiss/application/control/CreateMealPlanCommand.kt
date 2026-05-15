package software.ulpgc.cheffskiss.application.control

import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlan
import software.ulpgc.cheffskiss.domain.model.user.User
import java.util.UUID

class CreateMealPlanCommand(
    private val port: MealPlanRepository,
    private val userId: String,
    private val name: String
) : Command {
    override suspend fun execute() {
        port.createMealPlan(
            MealPlan(
                id = UUID.randomUUID(),
                name = name,
                creator = User(UUID.nameUUIDFromBytes(userId.toByteArray()))
            )
        )
    }
}
