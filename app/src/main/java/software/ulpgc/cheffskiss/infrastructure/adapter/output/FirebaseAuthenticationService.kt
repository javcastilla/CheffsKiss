package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import software.ulpgc.cheffskiss.application.port.output.LoginPort
import software.ulpgc.cheffskiss.application.port.output.LogoutPort
import software.ulpgc.cheffskiss.application.port.output.RegisterPort
import software.ulpgc.cheffskiss.application.port.output.CurrentUserPort

import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.domain.model.User
import software.ulpgc.cheffskiss.domain.port.output.UserRepository
import java.util.UUID

class FirebaseAuthenticationService :LoginPort, RegisterPort, LogoutPort , CurrentUserPort {

    override suspend fun register(
        email: String,
        password: String,
        username: String,
        description: String?,
        image: String?
    ): UUID? {
        val userId = Firebase.auth
            .createUserWithEmailAndPassword(email, password)
            .await().user?.uid ?: error("No UID returned")

        Firebase.firestore.collection("Username")
            .document(username).set(userNameHashMap(userId)).await()

        Firebase.firestore.collection("Users")
            .document(userId).set(userHashMap(email, description, image, username)).await()

        return try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            UUID.nameUUIDFromBytes(userId.toByteArray(Charsets.UTF_8))
        }
    }

    private fun userHashMap(
        email: String,
        description: String?,
        image: String?,
        username:String
    ): HashMap<String, String?> {
        return hashMapOf(
            "email" to email,
            "image" to image,
            "description" to description,
            "username" to username
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

    override fun getCurrentUser(): String? {
        val uid = Firebase.auth.currentUser?.uid ?: return null
        return uid
    }

    fun firebaseUidToUUID(firebaseUid: String): UUID {
        return UUID.nameUUIDFromBytes(firebaseUid.toByteArray(Charsets.UTF_8))
    }

}
interface RegisterUserInput{
    fun email(): String
    fun password(): String
    fun username(): String
    fun description(): String?
    fun image(): String
}