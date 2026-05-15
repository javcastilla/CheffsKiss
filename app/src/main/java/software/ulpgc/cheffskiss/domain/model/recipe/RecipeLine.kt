package software.ulpgc.cheffskiss.domain.model.recipe

import software.ulpgc.cheffskiss.domain.enum.Measurement
import java.util.UUID

data class RecipeLine(
    val id: UUID,
    val amount: Int,
    val ingredient: Ingredient? = null,
    val measurement: Measurement? = null,
) {
    fun with(ingredient: Ingredient): RecipeLine = copy(ingredient = ingredient)
    fun and(amount: Int): RecipeLine = copy(amount = amount)
    fun with(measurement: Measurement): RecipeLine = copy(measurement = measurement)
}