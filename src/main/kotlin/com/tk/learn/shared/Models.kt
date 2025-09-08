package com.tk.learn.shared

import org.valiktor.functions.*
import org.valiktor.validate

//DTO classes
data class CreateUserRequest(val name: String){
    init {
        validate(this){
            validate(CreateUserRequest::name).isNotNull().isNotBlank()
        }
    }
}
data class UpdateUserRequest(val name: String){
    init {
        validate(this){
            validate(UpdateUserRequest::name).isNotNull().isNotBlank()
        }
    }
}

//DAO classes
data class User(val userId: Int, val name: String){
    init {
        validate(this){
            validate(User::userId).isNotNull().hasDigits().isGreaterThan(0)
            validate(User::name).isNotBlank().doesNotMatch(Regex("^\\d+$"))
        }
    }
}
//Config classes
data class AppConfig(val database: DatabaseConfig){
    init {
        validate(this){
            validate(AppConfig::database).isNotNull()
        }
    }
}
data class DatabaseConfig(val url: String, val driver: String , val username: String, val password: String){
    init {
        validate(this){
            validate(DatabaseConfig::url).isNotBlank()
            validate(DatabaseConfig::driver).isNotBlank()
            validate(DatabaseConfig::username).isNotBlank()
        }
    }
}
