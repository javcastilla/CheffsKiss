package software.ulpgc.cheffskiss.domain.control

import java.util.UUID

interface Command {
    suspend fun execute()
}
