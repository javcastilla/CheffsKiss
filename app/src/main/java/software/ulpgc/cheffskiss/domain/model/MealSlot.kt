package software.ulpgc.cheffskiss.domain.model

import java.util.UUID

data class MealSlot(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val startTime: String,   // "HH:mm"
    val endTime: String,     // "HH:mm"
    val recipeId: UUID? = null,
    val colorIndex: Int = 0  // index into SLOT_COLORS palette
)
