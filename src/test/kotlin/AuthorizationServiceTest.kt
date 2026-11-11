package com.est.Security

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthorizationServiceTest {
    private fun newDatabase(): R2dbcDatabase {
        val databaseName = "authorization_test_" + UUID.randomUUID().toString().replace("-", "")
        return R2dbcDatabase.connect(
            url = "r2dbc:h2:file:///./build/$databaseName",
            user = "sa",
            password = ""
        )
    }

    private suspend fun createAccount(
        database: R2dbcDatabase,
        accountId: String,
        roles: List<String>,
        scopedRole: String? = null,
        scopeType: String? = null,
        scopeId: String? = null,
        isActive: Boolean = true
    ) {
        val now = System.currentTimeMillis()
        suspendTransaction(database) {
            Accounts.insert {
                it[Accounts.id] = accountId
                it[Accounts.emailNormalized] = "$accountId@example.com"
                it[Accounts.passwordHash] = "not-used-in-this-test"
                it[Accounts.isActive] = isActive
                it[Accounts.createdAt] = now
                it[Accounts.updatedAt] = now
            }

            roles.forEach { role ->
                AccountRoles.insert {
                    it[AccountRoles.accountId] = accountId
                    it[AccountRoles.roleCode] = role
                    it[AccountRoles.assignedAt] = now
                }
            }

            if (scopedRole != null && scopeType != null && scopeId != null) {
                RoleScopes.insert {
                    it[RoleScopes.id] = UUID.randomUUID().toString()
                    it[RoleScopes.accountId] = accountId
                    it[RoleScopes.roleCode] = scopedRole
                    it[RoleScopes.scopeType] = scopeType
                    it[RoleScopes.scopeId] = scopeId
                    it[RoleScopes.createdAt] = now
                }
            }
        }
    }

    @Test
    fun managerCanAccessOnlyTheAssignedTeamScope() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val accountId = UUID.randomUUID().toString()
        createAccount(
            database,
            accountId,
            roles = listOf("GESTOR_EQUIPA"),
            scopedRole = "GESTOR_EQUIPA",
            scopeType = "EQUIPA",
            scopeId = "equipa-10"
        )

        val authorization = AuthorizationService(database)
        assertTrue(authorization.canAccess(accountId, "GESTOR_EQUIPA", "EQUIPA", "equipa-10"))
        assertFalse(authorization.canAccess(accountId, "GESTOR_EQUIPA", "EQUIPA", "equipa-11"))
        assertEquals(
            setOf("equipa-10"),
            authorization.accessibleScopeIds(accountId, "GESTOR_EQUIPA", "EQUIPA")
        )
    }

    @Test
    fun scopeForAnotherRoleCannotGrantTheRequiredRoleAccess() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val accountId = UUID.randomUUID().toString()
        createAccount(
            database,
            accountId,
            roles = listOf("GESTOR_EQUIPA", "JOGADOR"),
            scopedRole = "JOGADOR",
            scopeType = "JOGADOR",
            scopeId = "jogador-7"
        )

        val authorization = AuthorizationService(database)
        assertFalse(authorization.canAccess(accountId, "GESTOR_EQUIPA", "EQUIPA", "equipa-10"))
        assertTrue(authorization.canAccess(accountId, "JOGADOR", "JOGADOR", "jogador-7"))
    }

    @Test
    fun organizerCanAccessOnlyTheAssignedEventScope() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val accountId = UUID.randomUUID().toString()
        createAccount(
            database,
            accountId,
            roles = listOf("ORGANIZADOR"),
            scopedRole = "ORGANIZADOR",
            scopeType = "EVENTO",
            scopeId = "evento-21"
        )

        val authorization = AuthorizationService(database)
        assertTrue(authorization.canAccess(accountId, "ORGANIZADOR", "EVENTO", "evento-21"))
        assertFalse(authorization.canAccess(accountId, "ORGANIZADOR", "EVENTO", "evento-22"))
        assertEquals(
            setOf("evento-21"),
            authorization.accessibleScopeIds(accountId, "ORGANIZADOR", "EVENTO")
        )
    }

    @Test
    fun administratorHasGlobalAccessWithoutResourceScope() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val accountId = UUID.randomUUID().toString()
        createAccount(database, accountId, roles = listOf("ADMINISTRADOR"))

        val authorization = AuthorizationService(database)
        assertTrue(authorization.canAccess(accountId, "GESTOR_EQUIPA", "EQUIPA", "any-team"))
        assertEquals(
            null,
            authorization.accessibleScopeIds(accountId, "GESTOR_EQUIPA", "EQUIPA")
        )
    }
    @Test
    fun inactiveAdministratorCannotPassGlobalRoleOrScopeChecks() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val accountId = UUID.randomUUID().toString()
        createAccount(
            database,
            accountId,
            roles = listOf("ADMINISTRADOR"),
            isActive = false
        )

        val authorization = AuthorizationService(database)
        assertFalse(authorization.hasGlobalRole(accountId, "ADMINISTRADOR"))
        assertFalse(authorization.canAccess(accountId, "GESTOR_EQUIPA", "EQUIPA", "any-team"))
        assertEquals(
            emptySet(),
            authorization.accessibleScopeIds(accountId, "GESTOR_EQUIPA", "EQUIPA")
        )
    }

}
