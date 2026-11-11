package com.est.DBUtils

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

object DBUtils {
    private val appEnvironment = System.getenv("APP_ENV")
        ?.trim()
        ?.lowercase()
        ?: "development"

    private fun requiredProductionValue(name: String): String {
        val value = System.getenv(name)?.takeIf { it.isNotBlank() }
        if (appEnvironment == "production" && value == null) {
            error("Missing required environment variable: $name")
        }
        return value.orEmpty()
    }

    private val databaseUrl = System.getenv("BASKET_HUB_DB_URL")
        ?.takeIf { it.isNotBlank() }
        ?: if (appEnvironment == "production") {
            error("BASKET_HUB_DB_URL must be configured in production")
        } else {
            "r2dbc:h2:file:///./h2"
        }

    private val databaseUser = requiredProductionValue("BASKET_HUB_DB_USER")
        .ifBlank { "root" }

    private val databasePassword = requiredProductionValue("BASKET_HUB_DB_PASSWORD")

    // Local development keeps the existing H2 defaults. Production must provide
    // an explicit URL and non-empty credentials through the deployment environment.
    val database: R2dbcDatabase = R2dbcDatabase.connect(
        url = databaseUrl,
        user = databaseUser,
        password = databasePassword
    )
}
