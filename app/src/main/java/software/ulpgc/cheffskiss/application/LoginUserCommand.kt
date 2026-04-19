package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.Authenticator
import software.ulpgc.cheffskiss.domain.control.Command

class LoginUserCommand(private val authenticator: Authenticator, private val loginInput: LoginInput) : Command {
    override suspend fun execute() {
         authenticator.login(loginInput.email(), loginInput.password())
    }
}
interface LoginInput{
    fun email(): String
    fun password(): String
}