package software.ulpgc.cheffskiss.domain.model.mealplan

import software.ulpgc.cheffskiss.domain.enum.MealType
import software.ulpgc.cheffskiss.domain.enum.WeekDay

val mealSlotComparator: Comparator<MealSlot> =
    compareBy<MealSlot> { it.day.ordinal }.thenBy { it.mealType.mealOrder }

private val MealType.mealOrder: Int
    get() = when (this) {
        MealType.BREAKFAST -> 0
        MealType.LUNCH -> 1
        MealType.DINNER -> 2
        MealType.SNACK -> 3
    }

fun List<MealSlot>.sortedBySchedule(): List<MealSlot> = sortedWith(mealSlotComparator)

fun MealPlan.sortedSlots(): MealPlan = copy(mealSlots = mealSlots.sortedBySchedule())

fun MealPlan.slotsForDay(day: WeekDay): List<MealSlot> =
    mealSlots.filter { it.day == day }.sortedBySchedule()
