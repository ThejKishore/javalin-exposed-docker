package com.tk.learn

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    override val primaryKey = PrimaryKey(id)
}

object Db {
    fun init(databaseConfig: DatabaseConfig) {
        // Connect to in-memory H2 database
        Database.connect(
            url = databaseConfig.url,
            driver = databaseConfig.driver,
            user = databaseConfig.username,
            password = databaseConfig.password
        )
        initializeData()
    }



    // Overload for tests: use default in-memory config
    fun init() {
        init(
            DatabaseConfig(
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                driver = "org.h2.Driver",
                username = "sa",
                password = ""
            )
        )
        initializeData()
    }

    private fun initializeData() {
        transaction {
            SchemaUtils.create(Users)
            // seed one row if empty
            if (Users.selectAll().empty()) {
                Users.insert { it[name] = "Alice" }
                Users.insert { it[name] = "Bob" }
            }
        }
    }

    fun listUsers(): List<User> = transaction {
        Users.selectAll().map { User(it[Users.id], it[Users.name]) }
    }

    fun createUser(name: String): User = transaction {
        val newId = Users.insert { it[Users.name] = name } get Users.id
        User(newId, name)
    }

    fun getUser(id: Int): User? = transaction {
        Users.select(Users.id, Users.name)
            .where { Users.id eq id }.limit(1)
            .firstOrNull()
            ?.let { User(it[Users.id], it[Users.name]) }
    }

    fun updateUser(id: Int, name: String): User? = transaction {
        val updated = Users.update({ Users.id eq id }) { it[Users.name] = name }
        if (updated > 0) getUser(id) else null
    }

    fun deleteUser(id: Int): Boolean = transaction {
        Users.deleteWhere { Users.id eq id } > 0
    }
}

