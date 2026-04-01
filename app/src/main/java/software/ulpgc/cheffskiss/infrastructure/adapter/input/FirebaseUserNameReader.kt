package software.ulpgc.cheffskiss.infrastructure.adapter.input

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.domain.model.UserName
import software.ulpgc.cheffskiss.domain.port.input.UserNameReader

class FirebaseUserNameReader : UserNameReader {
    override suspend fun exist(value: UserName): Boolean {
        return Firebase.firestore.collection("Username").document(value.value).get().await().exists()
    }
    override suspend fun getUsernameByUid(uid: String): String {
        val snapshot = Firebase.firestore
            .collection("Username")
            .whereEqualTo("UUID", uid)
            .get()
            .await()
        android.util.Log.d("UserNameReader", "uid buscado: $uid")

        android.util.Log.d("UserNameReader", "docs encontrados: ${snapshot.documents.size}")
        return snapshot.documents.firstOrNull()?.id ?:""
    }
}