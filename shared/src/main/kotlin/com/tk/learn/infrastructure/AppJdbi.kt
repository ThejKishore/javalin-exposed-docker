package com.tk.learn.infrastructure

import com.tk.learn.shared.DatabaseConfig
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin

object AppJdbi {
    private lateinit var jdbi: Jdbi

    fun init(databaseConfig: DatabaseConfig): Jdbi {
        val cfg = com.zaxxer.hikari.HikariConfig().apply {
            jdbcUrl = databaseConfig.url
            username = databaseConfig.username
            password = databaseConfig.password
            driverClassName = databaseConfig.driver
            maximumPoolSize = 10
            minimumIdle = 2
            poolName = "shared-hikari-pool"
        }
        val ds = com.zaxxer.hikari.HikariDataSource(cfg)
        jdbi = Jdbi.create(ds)
        // Enable Kotlin support for constructor mapping to Kotlin data classes
        jdbi.installPlugin(KotlinPlugin())
        return jdbi
    }

    fun getJdbi(): Jdbi = jdbi

    // Keep for backward compatibility with tests
    fun createSchema() = jdbi.useHandle<Exception> {
        it.execute("create table if not exists users (id INT generated always as identity primary key, name varchar(255) not null)")
        val count = it.createQuery("select count(*) from users").mapTo(Int::class.java).one()
        if (count == 0) {
            it.execute("INSERT INTO users (name) VALUES (?)", "Alice")
            it.createUpdate("INSERT INTO users (name) VALUES (?)")
                .bind(0, "Bob")
                .execute()
            it.createUpdate("INSERT INTO users (name) VALUES (:name)")
                .bind("name", "Clarice")
                .execute()
            it.createUpdate("INSERT INTO users (name) VALUES (:name)")
                .bind("name", "David")
                .execute()
        }
    }
}
