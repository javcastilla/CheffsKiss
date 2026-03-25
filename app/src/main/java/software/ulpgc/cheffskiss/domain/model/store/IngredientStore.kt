package software.ulpgc.cheffskiss.domain.model.store

import software.ulpgc.cheffskiss.domain.model.Ingredient
import java.util.UUID

class IngredientStore {
    private val ingredients: MutableMap<UUID, Ingredient> = HashMap();

    fun save(ingredient: Ingredient) {
        ingredients[ingredient.id] = (ingredient);
    }

    fun get(id: UUID): Ingredient? {
        return ingredients[id];
    }
}