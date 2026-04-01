package software.ulpgc.cheffskiss.domain.port.output

import software.ulpgc.cheffskiss.domain.model.User
import java.util.UUID

interface UserRepository {
    suspend fun save(user: User)

}

