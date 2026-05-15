package software.ulpgc.cheffskiss.domain.store

import software.ulpgc.cheffskiss.domain.model.User
import java.util.UUID

interface UserStore {
    fun with(id: UUID): java.util.Optional<User>
    fun named(username: String): java.util.Optional<User>
    fun add(user: User): UserStore
}