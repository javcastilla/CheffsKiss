package software.ulpgc.cheffskiss.domain.model

import software.ulpgc.cheffskiss.domain.enum.MealType
import software.ulpgc.cheffskiss.domain.enum.WeekDay
import java.util.UUID

data class MealSlot(
    val id: UUID,
    val day: WeekDay,
    val mealType: MealType,
    val recipe: Recipe? = null,
)