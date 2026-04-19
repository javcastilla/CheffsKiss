package software.ulpgc.cheffskiss.domain.model.vo

import java.util.UUID

data class RecipeLine(
    val id: UUID = UUID.randomUUID(),
    val ingredientId: UUID,
    val amount: Int,
    val measurement: Measurement
) {
    init {
        require(amount > 0) { "Amount must be greater than zero" }
    }
}