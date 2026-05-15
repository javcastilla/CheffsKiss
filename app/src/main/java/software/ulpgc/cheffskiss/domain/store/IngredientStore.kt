package software.ulpgc.cheffskiss.domain.store

import software.ulpgc.cheffskiss.domain.model.Ingredient
import software.ulpgc.cheffskiss.domain.model.RecipeLine
import java.util.stream.Stream

interface IngredientStore {
    fun of(recipeLine: RecipeLine): Stream<Ingredient>
    fun add(ingredient: Ingredient): IngredientStore
    fun named(name: String): java.util.Optional<Ingredient>
}