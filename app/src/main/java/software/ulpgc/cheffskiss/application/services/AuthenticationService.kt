package software.ulpgc.cheffskiss.application.services

import software.ulpgc.cheffskiss.application.port.output.LoginPort
import software.ulpgc.cheffskiss.application.port.output.RegisterPort

class AuthenticationService(private val logInPort: LoginPort, private val registerPort: RegisterPort) {
    suspend fun authenticate(email:String, password:String):Boolean{
        return logInPort.login(email, password)
    }
    suspend fun register(email: String, password: String, username: String, description: String?, image: String): Boolean {
        return registerPort.register(email, password, username, description, image) != null
    }
    suspend fun logout(){
        TODO("Not yet implemented")
    }
}
