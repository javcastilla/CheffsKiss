package software.ulpgc.cheffskiss.domain.model

class UserName(val value: String) {

    suspend fun isValid(value: String){
        require(value.isNotBlank()){"Fill the space"}
        require(value.length <= 3){"The name is too short"}
    }
}
