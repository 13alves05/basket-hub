package com.est.Security

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoleManagementServiceTest {
    private fun newDatabase(): R2dbcDatabase {
        val databaseName = "role_management_test_" + UUID.randomUUID().toString().replace("-", "")
        return R2dbcDatabase.connect(
            url = "r2dbc:h2:file:///./build/$databaseName",
            user = "sa",
            password = ""
        )
    }

    private suspend fun createAccount(database: R2dbcDatabase, id: String) {
        val now = System.currentTimeMillis()
        suspendTransaction(database) {
            Accounts.insert {
                it[Accounts.id] = id
                it[Accounts.emailNormalized] = "$id@example.com"
                it[Accounts.passwordHash] = "not-used-in-this-test"
                it[Accounts.isActive] = true
                it[Accounts.createdAt] = now
                it[Accounts.updatedAt] = now
            }
        }
    }

    private suspend fun createAdministrator(database: R2dbcDatabase): String {
        val token = "local-development-bootstrap-token-32-bytes"
        val adminId = AdminBootstrapService(database, token)
        val result = adminId.createFirstAdministrator(
            providedToken = token,
            email = "admin@example.com",
            password = "BasketHub-Seguro-2026"
        )
        assertEquals(BootstrapResult.CREATED, result)
        return suspendTransaction(database) {
            Accounts.selectAll()
                .where { Accounts.emailNormalized eq "admin@example.com" }
                .map { it[Accounts.id] }
                .singleOrNull()!!
        }
    }

    @Test
    fun administratorCanAssignRoleAndResourceScope() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val adminId = createAdministrator(database)
        val targetId = UUID.randomUUID().toString()
        createAccount(database, targetId)

        val management = RoleManagementService(database)
        assertEquals(
            RoleManagementResult.SUCCESS,
            management.assignRole(adminId, targetId, "GESTOR_EQUIPA")
        )
        assertEquals(
            RoleManagementResult.SUCCESS,
            management.assignScope(adminId, targetId, "GESTOR_EQUIPA", "EQUIPA", "equipa-10")
        )

        val authorization = AuthorizationService(database)
        assertTrue(authorization.canAccess(targetId, "GESTOR_EQUIPA", "EQUIPA", "equipa-10"))
        assertFalse(authorization.canAccess(targetId, "GESTOR_EQUIPA", "EQUIPA", "equipa-11"))

        assertEquals(
            RoleManagementResult.SUCCESS,
            management.removeRole(adminId, targetId, "GESTOR_EQUIPA")
        )
        assertEquals(
            RoleManagementResult.SUCCESS,
            management.assignRole(adminId, targetId, "GESTOR_EQUIPA")
        )
        assertFalse(
            authorization.canAccess(targetId, "GESTOR_EQUIPA", "EQUIPA", "equipa-10"),
            "A remoção e nova atribuição de um papel não podem reativar âmbitos antigos."
        )
    }

    @Test
    fun nonAdministratorCannotAssignRolesAndLastAdministratorCannotBeRemoved() = runBlocking {
        val database = newDatabase()
        SecuritySchema.create(database)
        val adminId = createAdministrator(database)
        val targetId = UUID.randomUUID().toString()
        createAccount(database, targetId)

        val management = RoleManagementService(database)
        assertEquals(
            RoleManagementResult.FORBIDDEN,
            management.assignRole(targetId, adminId, "JOGADOR")
        )
        assertEquals(
            RoleManagementResult.LAST_ADMIN_PROTECTED,
            management.removeRole(adminId, adminId, "ADMINISTRADOR")
        )
    }
}
