package software.ulpgc.cheffskiss.infrastructure.adapter.output

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.ulpgc.cheffskiss.application.port.FocusSessionRepository
import software.ulpgc.cheffskiss.domain.model.focus.FocusSession
import java.util.UUID

class LocalFocusSessionRepository(
    context: Context,
) : FocusSessionRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun save(session: FocusSession): Unit = withContext(Dispatchers.IO) {
        val id = session.recipeId.toString()
        prefs.edit()
            .putInt(stepIndexKey(id), session.currentStepIndex)
            .putString(completedKey(id), session.completedStepIds.joinToString(",") { it.toString() })
            .putLong(startedKey(id), session.startedAtEpochMs)
            .putLong(elapsedKey(id), session.elapsedMs)
            .putBoolean(screenOnKey(id), session.keepScreenOn)
            .apply()
    }

    override suspend fun load(recipeId: UUID): FocusSession? = withContext(Dispatchers.IO) {
        val id = recipeId.toString()
        if (!prefs.contains(stepIndexKey(id))) return@withContext null
        val completedRaw = prefs.getString(completedKey(id), "").orEmpty()
        val completed = completedRaw
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            .toSet()
        FocusSession(
            recipeId = recipeId,
            currentStepIndex = prefs.getInt(stepIndexKey(id), 0),
            completedStepIds = completed,
            startedAtEpochMs = prefs.getLong(startedKey(id), System.currentTimeMillis()),
            elapsedMs = prefs.getLong(elapsedKey(id), 0L),
            keepScreenOn = prefs.getBoolean(screenOnKey(id), true),
        )
    }

    override suspend fun clear(recipeId: UUID): Unit = withContext(Dispatchers.IO) {
        val id = recipeId.toString()
        prefs.edit()
            .remove(stepIndexKey(id))
            .remove(completedKey(id))
            .remove(startedKey(id))
            .remove(elapsedKey(id))
            .remove(screenOnKey(id))
            .apply()
    }

    private fun stepIndexKey(recipeId: String) = "focus.$recipeId.step_index"
    private fun completedKey(recipeId: String) = "focus.$recipeId.completed"
    private fun startedKey(recipeId: String) = "focus.$recipeId.started"
    private fun elapsedKey(recipeId: String) = "focus.$recipeId.elapsed"
    private fun screenOnKey(recipeId: String) = "focus.$recipeId.screen_on"

    companion object {
        private const val PREFS_NAME = "focus_sessions"
    }
}
