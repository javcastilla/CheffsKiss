package software.ulpgc.cheffskiss.domain.store

import software.ulpgc.cheffskiss.domain.model.RecipeList
import software.ulpgc.cheffskiss.domain.model.user.User
import java.util.stream.Stream

interface RecipeListStore {
    fun of(user: User): Stream<RecipeList>
    fun add(recipeList: RecipeList): RecipeListStore
}