package software.ulpgc.cheffskiss.domain.model.vo

import java.util.UUID

data class RecipeLine(
    val ingredientId: UUID,
    val amount: Double,
    val measurement: Measurement
) {
    init {
        require(amount > 0)
    }
}

enum class Measurement {
    UNIT, KILOGRAM, GRAM, LITRE, MILLILITRE, CUP,
    TABLESPOON, TEASPOON, SLICE, PINCH, TO_TASTE
}