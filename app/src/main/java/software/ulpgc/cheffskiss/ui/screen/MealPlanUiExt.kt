package software.ulpgc.cheffskiss.ui.screen

import software.ulpgc.cheffskiss.domain.enum.MealType
import software.ulpgc.cheffskiss.domain.enum.WeekDay
import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlan
import software.ulpgc.cheffskiss.domain.model.mealplan.MealSlot
import software.ulpgc.cheffskiss.domain.model.mealplan.slotsForDay

val WeekDay.shortName: String
    get() = when (this) {
        WeekDay.MONDAY -> "Mon"
        WeekDay.TUESDAY -> "Tue"
        WeekDay.WEDNESDAY -> "Wed"
        WeekDay.THURSDAY -> "Thu"
        WeekDay.FRIDAY -> "Fri"
        WeekDay.SATURDAY -> "Sat"
        WeekDay.SUNDAY -> "Sun"
    }

val WeekDay.displayName: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

fun MealType.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

fun MealPlan.slotsFor(day: WeekDay): List<MealSlot> = slotsForDay(day)

fun MealPlan.daysWithSlotsCount(): Int = WeekDay.entries.count { slotsForDay(it).isNotEmpty() }

fun mealSlotColor(slot: MealSlot): androidx.compose.ui.graphics.Color =
    SLOT_COLORS[slot.mealType.ordinal % SLOT_COLORS.size]
