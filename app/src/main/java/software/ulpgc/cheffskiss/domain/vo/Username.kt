package software.ulpgc.cheffskiss.domain.vo

@JvmInline
value class Username private constructor(val value: String) {
    companion object {
        private val VALID_PATTERN = Regex("^[a-zA-Z0-9_]{3,30}$")

        fun of(value: String): Result<Username> =
            if (VALID_PATTERN.matches(value)) Result.success(Username(value))
            else Result.failure(IllegalArgumentException("Invalid username: $value"))
    }
}