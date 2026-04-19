package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.LogoutClient
import software.ulpgc.cheffskiss.domain.control.Command

class LogoutUserCommand(private val logOutPort: LogoutClient): Command {


    override suspend fun execute() {
         logOutPort.logout()
    }
}