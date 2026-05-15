package software.ulpgc.cheffskiss.application.control

import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.MealPlan
import java.util.UUID

class CreateMealPlanCommand(
    private val port: MealPlanRepository,
    private val userId: String,
    private val name: String
) : Command {
    override suspend fun execute() {
        port.createMealPlan(
            MealPlan(
                userId = UUID.nameUUIDFromBytes(userId.toByteArray()),
                name = name,
                days = Weekday.entries.associateWith { emptyList() }
            )
        )
    }
}
