package software.ulpgc.cheffskiss.domain.model.focus

import java.util.UUID

enum class FocusPhase {
    INTRO,
    STEP,
    COMPLETE,
}

data class FocusSession(
    val recipeId: UUID,
    val currentStepIndex: Int,
    val completedStepIds: Set<UUID> = emptySet(),
    val startedAtEpochMs: Long,
    val elapsedMs: Long = 0L,
    val keepScreenOn: Boolean = true,
)

data class FocusCapabilities(
    val stepCount: Int,
    val timedStepCount: Int,
    val mediaStepCount: Int,
    val totalDurationMinutes: Long,
) {
  companion object {
      fun from(
          stepCount: Int,
          timedStepCount: Int,
          mediaStepCount: Int,
          totalDurationMinutes: Long,
      ): FocusCapabilities = FocusCapabilities(
          stepCount = stepCount,
          timedStepCount = timedStepCount,
          mediaStepCount = mediaStepCount,
          totalDurationMinutes = totalDurationMinutes,
      )
  }
}
