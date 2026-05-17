package software.ulpgc.cheffskiss.application.services

enum class IngredientSearchMode {
    /** Matches if the query appears as a substring in name, aliases, tags, etc. */
    DIRECT,

    /** Each word in the query must match somewhere (any order) — reverse / pantry-style lookup. */
    REVERSE,
}

enum class ExploreSearchMode {
    BY_TITLE,
    BY_INGREDIENTS,
}
