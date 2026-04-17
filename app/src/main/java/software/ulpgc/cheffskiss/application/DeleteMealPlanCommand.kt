package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.output.MealPlanPort
import software.ulpgc.cheffskiss.domain.control.Command
import java.util.UUID

class DeleteMealPlanCommand(
    private val port: MealPlanPort,
    private val userId: String,
    private val planId: UUID
) : Command {
    override suspend fun execute() {
        port.deleteMealPlan(
            planId = planId,
            userId = UUID.nameUUIDFromBytes(userId.toByteArray())
        )
    }
}
