package software.ulpgc.cheffskiss.application.services

import java.util.UUID

object UserIds {
    fun creatorIdFromFirebaseUid(firebaseUid: String): UUID =
        UUID.nameUUIDFromBytes(firebaseUid.toByteArray(Charsets.UTF_8))

    fun creatorIdStringFromFirebaseUid(firebaseUid: String): String =
        creatorIdFromFirebaseUid(firebaseUid).toString()
}
