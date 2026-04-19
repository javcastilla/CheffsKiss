package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.domain.control.Command
import java.util.UUID

class SetActiveMealPlanCommand(
    private val port: MealPlanRepository,
    private val userId: String,
    private val planId: UUID
) : Command {
    override suspend fun execute() {
        port.setActivePlan(
            planId = planId,
            userId = UUID.nameUUIDFromBytes(userId.toByteArray())
        )
    }
}
