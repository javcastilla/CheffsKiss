package software.ulpgc.cheffskiss.application.control

import software.ulpgc.cheffskiss.application.port.Registrator
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.UserName
import software.ulpgc.cheffskiss.domain.port.input.UserNameReader

class RegisterUserCommand(
    private val userNameReader: UserNameReader,
    private val userPort: Registrator,
    private val registerUserInput: Input)
    : Command {


    override suspend fun execute() {
        require(!userNameReader.exist(getUserName())) { "Username already exists" }
        val uid=userPort.register(
            registerUserInput.email(),
            registerUserInput.password(),
            registerUserInput.username(),
            registerUserInput.description(),
            registerUserInput.image()
        )
        require(uid != null) { "Error registering user" }
    }

    private fun getUserName(): UserName {
        return UserName(registerUserInput.username())
    }
}

interface Input {
    fun email(): String
    fun password(): String
    fun username(): String
    fun description(): String?
    fun image(): String?
}
