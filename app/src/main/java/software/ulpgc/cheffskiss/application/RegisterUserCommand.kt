package software.ulpgc.cheffskiss.application

import software.ulpgc.cheffskiss.application.port.output.RegisterPort
import software.ulpgc.cheffskiss.domain.control.Command
import software.ulpgc.cheffskiss.domain.model.UserName
import software.ulpgc.cheffskiss.domain.port.input.UserNameReader
import java.util.UUID

class RegisterUserCommand(
    private val userNameReader: UserNameReader,
    private val userPort: RegisterPort,
    private val registerUserInput: Input)
    : Command {


    override suspend fun execute() {
        require(!userNameReader.exist(getUserName())){"Username already exists"}
        userPort.register(
            registerUserInput.email(),
            registerUserInput.password(),
            registerUserInput.username(),
            registerUserInput.description(),
            registerUserInput.image()
        )
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
    fun image(): String
}
