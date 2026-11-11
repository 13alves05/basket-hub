package com.est.Security

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminBootstrapServiceTest {
    private fun newDatabase(): R2dbcDatabase {
        val databaseName = "security_test_" + UUID.randomUUID().toString().replace("-", "")
        return R2dbcDatabase.connect(
            url = "r2dbc:h2:mem:///$databaseName",
            user = "sa",
            password = ""
        )
    }

    @Test
    fun bootstrapCreatesOneAdministratorAndRejectsASecondAttempt() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val token = "local-development-bootstrap-token-32-bytes"
        val service = AdminBootstrapService(database, token)

        val first = service.createFirstAdministrator(
            providedToken = token,
            email = "  Admin@Example.COM ",
            password = "BasketHub-Seguro-2026"
        )
        val second = service.createFirstAdministrator(
            providedToken = token,
            email = "other@example.com",
            password = "Outra-Palavra-Passe-2026"
        )

        assertEquals(BootstrapResult.CREATED, first)
        assertEquals(BootstrapResult.ALREADY_COMPLETED, second)

        val accounts = suspendTransaction(database) {
            Accounts.selectAll().toList()
        }
        val roles = suspendTransaction(database) {
            AccountRoles.selectAll().toList()
        }
        val auditEntries = suspendTransaction(database) {
            SecurityAuditLog.selectAll().toList()
        }

        assertEquals(1, accounts.size)
        assertEquals("admin@example.com", accounts.single()[Accounts.emailNormalized])
        assertTrue(accounts.single()[Accounts.passwordHash].startsWith("\$argon2id\$"))
        assertEquals(1, roles.size)
        assertEquals("ADMINISTRADOR", roles.single()[AccountRoles.roleCode])
        assertEquals(1, auditEntries.size)
        assertEquals("FIRST_ADMIN_BOOTSTRAP_COMPLETED", auditEntries.single()[SecurityAuditLog.eventType])
    }

    @Test
    fun bootstrapRejectsInvalidTokenWithoutCreatingAnAccount() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val service = AdminBootstrapService(database, "a-valid-local-token-with-at-least-32-bytes")

        val result = service.createFirstAdministrator(
            providedToken = "incorrect-token",
            email = "admin@example.com",
            password = "BasketHub-Seguro-2026"
        )

        assertEquals(BootstrapResult.INVALID_TOKEN, result)
        val accounts = suspendTransaction(database) {
            Accounts.selectAll().toList()
        }
        assertTrue(accounts.isEmpty())
    }

    @Test
    fun bootstrapRejectsWeakConfiguredToken() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val service = AdminBootstrapService(database, "too-short")

        val result = service.createFirstAdministrator(
            providedToken = "too-short",
            email = "admin@example.com",
            password = "BasketHub-Seguro-2026"
        )

        assertEquals(BootstrapResult.UNAVAILABLE, result)
        val accounts = suspendTransaction(database) {
            Accounts.selectAll().toList()
        }
        assertTrue(accounts.isEmpty())
    }
}
