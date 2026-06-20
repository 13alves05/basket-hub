package com.est.DBUtils

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

object DBUtils {

    // ligação única à base de dados
    val database: R2dbcDatabase = R2dbcDatabase.connect(
        url = "r2dbc:h2:file:///./h2",
        user = "root",
        password = ""
    )
}