package com.tk.learn

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin


object AppJdbi
{
    private lateinit var jdbi: Jdbi

    fun init(databaseConfig: DatabaseConfig) : Jdbi {
        jdbi= Jdbi.create(databaseConfig.url, databaseConfig.username, databaseConfig.password)
        // Enable Kotlin support for constructor mapping to Kotlin data classes
        jdbi.installPlugin(KotlinPlugin())
        return jdbi
    }

    fun getJdbi(): Jdbi = jdbi

    fun createSchema() = jdbi.useHandle<Exception> {
            // H2 in PostgreSQL mode: use generated always as identity for autoincrement
            it.execute("create table if not exists users (id INT generated always as identity primary key, name varchar(255) not null)")

            // Seed sample data only if table is empty
            val count = it.createQuery("select count(*) from users").mapTo(Int::class.java).one()
            if (count == 0) {
                // Inline positional parameters
                it.execute("INSERT INTO users (name) VALUES (?)",  "Alice")

                // Positional parameters
                it.createUpdate("INSERT INTO users (name) VALUES (?)")
                    .bind(0, "Bob")
                    .execute()

                // Named parameters
                it.createUpdate("INSERT INTO users (name) VALUES (:name)")
                    .bind("name", "Clarice")
                    .execute()

                // Named parameters from explicit binds
                it.createUpdate("INSERT INTO users (name) VALUES (:name)")
                    .bind("name", "David")
                    .execute()
            }
        }

}