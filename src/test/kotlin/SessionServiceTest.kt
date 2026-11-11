package com.est.Security

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionServiceTest {
    private fun newDatabase(): R2dbcDatabase {
        val databaseName = "session_test_" + UUID.randomUUID().toString().replace("-", "")
        return R2dbcDatabase.connect(
            url = "r2dbc:h2:file:///./build/$databaseName",
            user = "sa",
            password = ""
        )
    }

    @Test
    fun loginCreatesBearerSessionAndLogoutRevokesIt() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val bootstrapToken = "local-development-bootstrap-token-32-bytes"
        val password = "BasketHub-Seguro-2026"
        val bootstrap = AdminBootstrapService(database, bootstrapToken)

        assertEquals(
            BootstrapResult.CREATED,
            bootstrap.createFirstAdministrator(
                providedToken = bootstrapToken,
                email = "admin@example.com",
                password = password
            )
        )

        val sessions = SessionService(database)
        val (result, issued) = sessions.login(" ADMIN@EXAMPLE.COM ", password)

        assertEquals(LoginResult.AUTHENTICATED, result)
        assertNotNull(issued)
        assertTrue(issued.token.isNotBlank())
        assertTrue(issued.account.roles.contains("ADMINISTRADOR"))

        val authenticated = sessions.authenticate(issued.token)
        assertNotNull(authenticated)
        assertEquals("admin@example.com", authenticated.email)

        assertTrue(sessions.logout(issued.token))
        assertNull(sessions.authenticate(issued.token))
        assertTrue(!sessions.logout(issued.token))
    }

    @Test
    fun loginRejectsInvalidCredentials() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val bootstrapToken = "local-development-bootstrap-token-32-bytes"
        val bootstrap = AdminBootstrapService(database, bootstrapToken)

        bootstrap.createFirstAdministrator(
            providedToken = bootstrapToken,
            email = "admin@example.com",
            password = "BasketHub-Seguro-2026"
        )

        val (result, session) = SessionService(database).login(
            "admin@example.com",
            "palavra-passe-errada"
        )

        assertEquals(LoginResult.INVALID_CREDENTIALS, result)
        assertNull(session)
    }
}
