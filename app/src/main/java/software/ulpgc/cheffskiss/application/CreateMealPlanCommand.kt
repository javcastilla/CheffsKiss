package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.output.MealPlanPort
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.MealPlan
import software.ulpgc.cheffskiss.domain.model.Weekday
import java.util.UUID

class CreateMealPlanCommand(
    private val port: MealPlanPort,
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
