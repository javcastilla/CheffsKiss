package software.ulpgc.cheffskiss.application.services

import software.ulpgc.cheffskiss.application.port.Authenticator
import software.ulpgc.cheffskiss.application.port.Registrator

class AuthenticationService(private val logInPort: Authenticator, private val registrator: Registrator) {
    suspend fun authenticate(email:String, password:String):Boolean{
        return logInPort.login(email, password)
    }
    suspend fun register(email: String, password: String, username: String, description: String?, image: String): Boolean {
        return registrator.register(email, password, username, description, image) != null
    }
    suspend fun logout(){
        TODO("Not yet implemented")
    }
}
