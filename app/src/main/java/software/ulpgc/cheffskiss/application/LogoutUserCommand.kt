package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.output.LogoutPort
import software.ulpgc.cheffskiss.domain.control.Command

class LogoutUserCommand(private val logOutPort: LogoutPort): Command {


    override suspend fun execute() {
         logOutPort.logout()
    }
}