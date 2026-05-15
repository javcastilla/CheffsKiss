package software.ulpgc.cheffskiss.domain.store

import software.ulpgc.cheffskiss.domain.model.Recipe
import software.ulpgc.cheffskiss.domain.model.RecipeVersion
import software.ulpgc.cheffskiss.domain.model.User
import java.util.UUID
import java.util.stream.Stream

interface RecipeStore {
    fun of(user: User): Stream<Recipe>
    fun with(id: UUID): java.util.Optional<Recipe>
    fun add(recipe: Recipe): RecipeStore
    fun history(id: UUID): Stream<RecipeVersion>
}