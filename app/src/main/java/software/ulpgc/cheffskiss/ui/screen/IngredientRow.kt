package software.ulpgc.cheffskiss.ui.screen

import android.net.Uri

data class IngredientRow(
    val id: Int,
    val name: String = "",
    val amount: String = "",
    val unit: String = "UNIT",
    val ingredientId: String? = null
)

data class StepRow(
    val id: Int,
    val description: String = "",
    val duration: String = "",
    val imageUri: Uri? = null,
    val existingImageUrl: String? = null
)

val unitOptions = listOf("UNIT", "GRAM", "KG", "ML", "LITRE", "CUP", "TBSP", "TSP", "SLICE", "PINCH")

fun MutableList<IngredientRow>.addIngredientRow(nextId: Int) {
    add(IngredientRow(id = nextId))
}

fun MutableList<StepRow>.addStepRow(nextId: Int) {
    add(StepRow(id = nextId))
}