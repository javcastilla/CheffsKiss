package software.ulpgc.cheffskiss.ui.screen

import software.ulpgc.cheffskiss.domain.enum.Measurement
import software.ulpgc.cheffskiss.domain.model.recipe.Ingredient

fun Measurement.label(): String = when (this) {
    Measurement.UNIT -> "Unit"
    Measurement.KILO -> "Kilo"
    Measurement.LITER -> "Liter"
    Measurement.LB -> "Lb"
}

val Ingredient.displayCategory: String
    get() = categories.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: category
