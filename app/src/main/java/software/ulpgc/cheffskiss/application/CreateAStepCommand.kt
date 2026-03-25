package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.Step
import java.util.UUID
import kotlin.time.Duration

class CreateAStepCommand(val description: String,
                         val duration: Duration,
                         val cardinal: Int): Command {
    override suspend fun execute() {
        require(description.isNotBlank())
        require(cardinal>0)
        Step(UUID.randomUUID(), description, duration, cardinal)
    }
}