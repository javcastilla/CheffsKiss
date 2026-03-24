package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import software.ulpgc.cheffskiss.application.port.output.LoginPort
import software.ulpgc.cheffskiss.application.port.output.LogoutPort
import software.ulpgc.cheffskiss.application.port.output.RegisterPort
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseAuthenticationService :LoginPort, RegisterPort, LogoutPort {

    override suspend fun register(
        email: String,
        password: String,
        username: String,
        description: String?,
        image: String
    ): UUID? {
        try {
            val userId =Firebase.auth.createUserWithEmailAndPassword(email, password).await().user?.uid ?: error("No UID returned")
            Firebase.firestore.collection("Username").document(username).set(userNameHashMap(userId)).await()
            Firebase.firestore.collection("Users").document(userId).set(userHashMap(email,description,image)).await()
            return UUID.fromString(userId)

        }catch (e: Exception){
            println("Error al registrar: ${e.message}")
            return null
        }
    }

    private fun userHashMap(
        email: String,
        description: String?,
        image: String
    ): HashMap<String, String?> {
        return hashMapOf(
            "email" to email,
            "image" to image,
            "description" to description
        )
    }

    private fun userNameHashMap(userId: String): HashMap<String, String> {
        return hashMapOf(
            "UUID" to userId)
    }

    override suspend fun logout() {
        Firebase.auth.signOut()
    }

    override suspend fun login(email: String, password: String): Boolean {
        return Firebase.auth.signInWithEmailAndPassword(email, password).await().user != null
    }


}
interface RegisterUserInput{
    fun email(): String
    fun password(): String
    fun username(): String
    fun description(): String?
    fun image(): String
}