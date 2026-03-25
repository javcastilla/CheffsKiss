package software.ulpgc.cheffskiss.domain.model

class Username(val value: String) {

    suspend fun isValid(value: String){
        require(value.isNotBlank()){"Fill the space"}
        require(value.length >= 3){"The name is too short"}
    }
}
