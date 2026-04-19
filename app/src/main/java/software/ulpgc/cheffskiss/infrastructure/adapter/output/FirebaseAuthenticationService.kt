package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import software.ulpgc.cheffskiss.application.port.Authenticator
import software.ulpgc.cheffskiss.application.port.LogoutClient
import software.ulpgc.cheffskiss.application.port.Registrator
import software.ulpgc.cheffskiss.application.port.CurrentUserPort

import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseAuthenticationService :Authenticator, Registrator, LogoutClient , CurrentUserPort {

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

        val javaUuid = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            UUID.nameUUIDFromBytes(userId.toByteArray(Charsets.UTF_8))
        }

        Firebase.firestore.collection("Username")
            .document(username)
            .set(userNameHashMap(userId)).await()

        Firebase.firestore.collection("Users")
            .document(javaUuid.toString())
            .set(userHashMap(email, description, image, username)).await()

        return javaUuid
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