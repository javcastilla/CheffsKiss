package software.ulpgc.cheffskiss.infrastructure.adapter.input

import software.ulpgc.cheffskiss.domain.model.user.User
import software.ulpgc.cheffskiss.domain.port.input.UserReader
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.net.URI
import software.ulpgc.cheffskiss.domain.vo.Username
import software.ulpgc.cheffskiss.domain.vo.Description

class FirebaseUserReader: UserReader {
    override suspend fun getByEmail(email: String): User? {
        android.util.Log.d("UserReader", "email buscado: $email")
        val snapshot = Firebase.firestore.collection("Users").whereEqualTo("email", email).get().await()
        return snapshot.documents.firstOrNull()?.toUser()
    }
    private fun DocumentSnapshot.toUser(): User? {
        val idStr = id ?: return null
        val usernameStr = getString("username")
        val username = if (usernameStr != null) {
            Username.of(usernameStr).getOrNull() ?: return null
        } else {
            null
        }
        
        val imageStr = getString("image")
        val image = if (!imageStr.isNullOrEmpty()) {
            try {
                URI(imageStr)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
        
        val descriptionStr = getString("description")
        val description = if (!descriptionStr.isNullOrEmpty()) {
            Description(descriptionStr)
        } else {
            null
        }
        
        return User(
            id          = try {
                UUID.fromString(idStr)
            } catch (e: IllegalArgumentException) {
                UUID.nameUUIDFromBytes(idStr.toByteArray(Charsets.UTF_8))
            },
            username    = username,
            image       = image,
            description = description
        )
    }


    override suspend fun getByUid(uid: String): User? {
        val javaUuid = try {
            UUID.fromString(uid)
        } catch (e: IllegalArgumentException) {
            UUID.nameUUIDFromBytes(uid.toByteArray(Charsets.UTF_8))
        }

        val snapshot = Firebase.firestore
            .collection("Users")
            .document(javaUuid.toString())
            .get().await()

        return snapshot.toUser()
    }

}


