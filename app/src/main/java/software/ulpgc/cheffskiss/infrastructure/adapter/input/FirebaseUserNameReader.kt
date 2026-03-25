package software.ulpgc.cheffskiss.infrastructure.adapter.input

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.domain.model.Username
import software.ulpgc.cheffskiss.domain.port.input.UserNameReader

class FirebaseUserNameReader : UserNameReader {
    override suspend fun exist(value: Username): Boolean {
        return Firebase.firestore.collection("Username").document(value.value).get().await().exists()
    }
}