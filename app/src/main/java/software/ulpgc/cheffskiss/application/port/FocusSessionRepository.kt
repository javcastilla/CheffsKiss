package software.ulpgc.cheffskiss.application.port

import software.ulpgc.cheffskiss.domain.model.focus.FocusSession
import java.util.UUID

interface FocusSessionRepository {
    suspend fun save(session: FocusSession)
    suspend fun load(recipeId: UUID): FocusSession?
    suspend fun clear(recipeId: UUID)
}
