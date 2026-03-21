package software.ulpgc.cheffskiss.domain.model

data class Recipe(
    val id: String,
    val name: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val author: String
)
