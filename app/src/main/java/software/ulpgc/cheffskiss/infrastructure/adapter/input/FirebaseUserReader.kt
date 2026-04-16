package software.ulpgc.cheffskiss.infrastructure.adapter.input

import software.ulpgc.cheffskiss.domain.model.User
import software.ulpgc.cheffskiss.domain.port.input.UserReader
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.domain.model.UserName
import java.util.UUID

class FirebaseUserReader: UserReader {
    override suspend fun getByEmail(email: String): User? {
        android.util.Log.d("UserReader", "email buscado: $email")
        val snapshot = Firebase.firestore.collection("Users").whereEqualTo("email", email).get().await()
        return snapshot.documents.firstOrNull()?.toUser()
    }
    private fun DocumentSnapshot.toUser(): User? {
        val idStr = id ?: return null
        return User(
            id          = try {
                UUID.fromString(idStr)
            } catch (e: IllegalArgumentException) {
                UUID.nameUUIDFromBytes(idStr.toByteArray(Charsets.UTF_8))
            },
            username    = UserName(getString("username") ?: return null),
            image       = getString("image") ?: "",
            description = getString("description")
        )
    }


    override suspend fun getByUid(uid: String): User? {
        android.util.Log.d("UserReader", "uid buscado: $uid")
        val snapshot = Firebase.firestore.collection("Users").document(uid).get().await()
        return snapshot.toUser()
    }

}


