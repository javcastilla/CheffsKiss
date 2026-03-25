package software.ulpgc.cheffskiss.domain.model.store

import software.ulpgc.cheffskiss.domain.model.Ingredient

class IngredientStore {
    private val ingredients: MutableSet<Ingredient> = HashSet();

    fun save(ingredient: Ingredient) {
        ingredients.add(ingredient);
    }

    fun of(recipeLineStore: RecipeLineStore) {
        ingredients.stream()
            .
    }
}