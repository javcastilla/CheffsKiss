package software.ulpgc.cheffskiss.infrastructure.adapter.input

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.domain.port.input.UserNameReader
import software.ulpgc.cheffskiss.domain.vo.Username

class FirebaseUserNameReader : UserNameReader {
    override suspend fun exist(value: Username): Boolean {
        return Firebase.firestore.collection("Username").document(value.value).get().await().exists()
    }
    override suspend fun getUsernameByUid(uid: String): String {
        FirebaseUserReader().getByUid(uid)?.username?.value?.takeIf { it.isNotBlank() }?.let { return it }

        val snapshot = Firebase.firestore
            .collection("Username")
            .whereEqualTo("UUID", uid)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.id ?: ""
    }
}