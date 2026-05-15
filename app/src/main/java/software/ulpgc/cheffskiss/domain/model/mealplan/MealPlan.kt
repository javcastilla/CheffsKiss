package software.ulpgc.cheffskiss.domain.model.mealplan

import software.ulpgc.cheffskiss.domain.model.user.User
import java.util.UUID

data class MealPlan(
    val id: UUID,
    val version: Int = 0,
    val name: String,
    val mealSlots: List<MealSlot> = emptyList(),
    val creator: User? = null,
) {
    fun with(mealSlot: MealSlot): MealPlan = copy(mealSlots = mealSlots + mealSlot)
    fun named(name: String): MealPlan = copy(name = name)
    fun createdBy(user: User): MealPlan = copy(creator = user)
}