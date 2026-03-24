package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.output.LoginPort
import software.ulpgc.cheffskiss.domain.control.Command
import java.util.UUID

class LoginUserCommand(private val loginPort: LoginPort, private val loginInput: LoginInput) : Command {
    override suspend fun execute() {
         loginPort.login(loginInput.email(), loginInput.password())
    }
}
interface LoginInput{
    fun email(): String
    fun password(): String
}