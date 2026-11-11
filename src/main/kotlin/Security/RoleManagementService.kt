package com.est.Security

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.util.UUID

enum class RoleManagementResult {
    SUCCESS,
    FORBIDDEN,
    NOT_FOUND,
    INVALID_ROLE,
    INVALID_SCOPE,
    ROLE_REQUIRED,
    ALREADY_ASSIGNED,
    NOT_ASSIGNED,
    LAST_ADMIN_PROTECTED
}

/**
 * Administrative operations for assigning roles and resource scopes.
 * The caller must still authenticate the administrator at the HTTP boundary.
 */
class RoleManagementService(private val database: R2dbcDatabase) {
    private val supportedScopeTypes = setOf("EVENTO", "EQUIPA", "JOGADOR", "PATROCINADOR")

    suspend fun assignRole(
        actorAccountId: String,
        targetAccountId: String,
        roleCode: String
    ): RoleManagementResult = suspendTransaction(database) {
        if (!isActiveAdministrator(actorAccountId)) {
            return@suspendTransaction RoleManagementResult.FORBIDDEN
        }

        val targetExists = Accounts
            .selectAll()
            .where {
                (Accounts.id eq targetAccountId) and
                    (Accounts.isActive eq true)
            }
            .limit(1)
            .map { it[Accounts.id] }
            .singleOrNull() != null
        if (!targetExists) return@suspendTransaction RoleManagementResult.NOT_FOUND

        val roleExists = Roles
            .selectAll()
            .where { Roles.code eq roleCode }
            .limit(1)
            .map { it[Roles.code] }
            .singleOrNull() != null
        if (!roleExists) return@suspendTransaction RoleManagementResult.INVALID_ROLE

        val alreadyAssigned = AccountRoles
            .selectAll()
            .where {
                (AccountRoles.accountId eq targetAccountId) and
                    (AccountRoles.roleCode eq roleCode)
            }
            .limit(1)
            .map { it[AccountRoles.roleCode] }
            .singleOrNull() != null
        if (alreadyAssigned) return@suspendTransaction RoleManagementResult.ALREADY_ASSIGNED

        val now = System.currentTimeMillis()
        AccountRoles.insert {
            it[AccountRoles.accountId] = targetAccountId
            it[AccountRoles.roleCode] = roleCode
            it[AccountRoles.assignedAt] = now
        }
        audit(actorAccountId, "ROLE_ASSIGNED", "ACCOUNT_ROLE", "$targetAccountId:$roleCode", now)
        RoleManagementResult.SUCCESS
    }

    suspend fun removeRole(
        actorAccountId: String,
        targetAccountId: String,
        roleCode: String
    ): RoleManagementResult = suspendTransaction(database) {
        if (!isActiveAdministrator(actorAccountId)) {
            return@suspendTransaction RoleManagementResult.FORBIDDEN
        }

        val assignmentExists = AccountRoles
            .selectAll()
            .where {
                (AccountRoles.accountId eq targetAccountId) and
                    (AccountRoles.roleCode eq roleCode)
            }
            .limit(1)
            .map { it[AccountRoles.accountId] }
            .singleOrNull() != null
        if (!assignmentExists) return@suspendTransaction RoleManagementResult.NOT_ASSIGNED

        if (roleCode == "ADMINISTRADOR") {
            val administratorIds = AccountRoles
                .selectAll()
                .where { AccountRoles.roleCode eq "ADMINISTRADOR" }
                .map { it[AccountRoles.accountId] }
                .toList()
                .toSet()
            val activeAccountIds = Accounts
                .selectAll()
                .where { Accounts.isActive eq true }
                .map { it[Accounts.id] }
                .toList()
                .toSet()
            val activeAdministratorCount = administratorIds.intersect(activeAccountIds).size
            if (activeAdministratorCount <= 1) {
                return@suspendTransaction RoleManagementResult.LAST_ADMIN_PROTECTED
            }
        }

        AccountRoles.deleteWhere {
            (AccountRoles.accountId eq targetAccountId) and
                (AccountRoles.roleCode eq roleCode)
        }
        // Remove old scopes with the role so a future re-assignment cannot
        // silently reactivate permissions that had already been revoked.
        RoleScopes.deleteWhere {
            (RoleScopes.accountId eq targetAccountId) and
                (RoleScopes.roleCode eq roleCode)
        }
        val now = System.currentTimeMillis()
        audit(actorAccountId, "ROLE_REMOVED", "ACCOUNT_ROLE", "$targetAccountId:$roleCode", now)
        RoleManagementResult.SUCCESS
    }

    suspend fun assignScope(
        actorAccountId: String,
        targetAccountId: String,
        roleCode: String,
        scopeType: String,
        scopeId: String
    ): RoleManagementResult = suspendTransaction(database) {
        if (!isActiveAdministrator(actorAccountId)) {
            return@suspendTransaction RoleManagementResult.FORBIDDEN
        }
        if (scopeType !in supportedScopeTypes || scopeId.isBlank() || scopeId.length > 100) {
            return@suspendTransaction RoleManagementResult.INVALID_SCOPE
        }

        val roleAssignmentExists = AccountRoles
            .selectAll()
            .where {
                (AccountRoles.accountId eq targetAccountId) and
                    (AccountRoles.roleCode eq roleCode)
            }
            .limit(1)
            .map { it[AccountRoles.roleCode] }
            .singleOrNull() != null
        if (!roleAssignmentExists) return@suspendTransaction RoleManagementResult.ROLE_REQUIRED

        val existingScope = RoleScopes
            .selectAll()
            .where {
                (RoleScopes.accountId eq targetAccountId) and
                    (RoleScopes.roleCode eq roleCode) and
                    (RoleScopes.scopeType eq scopeType) and
                    (RoleScopes.scopeId eq scopeId)
            }
            .limit(1)
            .map { it[RoleScopes.id] }
            .singleOrNull() != null
        if (existingScope) return@suspendTransaction RoleManagementResult.ALREADY_ASSIGNED

        val now = System.currentTimeMillis()
        RoleScopes.insert {
            it[RoleScopes.id] = UUID.randomUUID().toString()
            it[RoleScopes.accountId] = targetAccountId
            it[RoleScopes.roleCode] = roleCode
            it[RoleScopes.scopeType] = scopeType
            it[RoleScopes.scopeId] = scopeId
            it[RoleScopes.createdAt] = now
        }
        audit(actorAccountId, "ROLE_SCOPE_ASSIGNED", "ROLE_SCOPE", "$targetAccountId:$roleCode:$scopeType:$scopeId", now)
        RoleManagementResult.SUCCESS
    }

    suspend fun revokeScope(
        actorAccountId: String,
        targetAccountId: String,
        roleCode: String,
        scopeType: String,
        scopeId: String
    ): RoleManagementResult = suspendTransaction(database) {
        if (!isActiveAdministrator(actorAccountId)) {
            return@suspendTransaction RoleManagementResult.FORBIDDEN
        }

        val scopeExists = RoleScopes
            .selectAll()
            .where {
                (RoleScopes.accountId eq targetAccountId) and
                    (RoleScopes.roleCode eq roleCode) and
                    (RoleScopes.scopeType eq scopeType) and
                    (RoleScopes.scopeId eq scopeId)
            }
            .limit(1)
            .map { it[RoleScopes.id] }
            .singleOrNull() != null
        if (!scopeExists) return@suspendTransaction RoleManagementResult.NOT_ASSIGNED

        RoleScopes.deleteWhere {
            (RoleScopes.accountId eq targetAccountId) and
                (RoleScopes.roleCode eq roleCode) and
                (RoleScopes.scopeType eq scopeType) and
                (RoleScopes.scopeId eq scopeId)
        }
        val now = System.currentTimeMillis()
        audit(actorAccountId, "ROLE_SCOPE_REVOKED", "ROLE_SCOPE", "$targetAccountId:$roleCode:$scopeType:$scopeId", now)
        RoleManagementResult.SUCCESS
    }

    private suspend fun isActiveAdministrator(accountId: String): Boolean {
        val active = Accounts
            .selectAll()
            .where { (Accounts.id eq accountId) and (Accounts.isActive eq true) }
            .limit(1)
            .map { it[Accounts.id] }
            .singleOrNull() != null
        if (!active) return false

        return AccountRoles
            .selectAll()
            .where {
                (AccountRoles.accountId eq accountId) and
                    (AccountRoles.roleCode eq "ADMINISTRADOR")
            }
            .limit(1)
            .map { it[AccountRoles.roleCode] }
            .singleOrNull() != null
    }

    private suspend fun audit(
        actorAccountId: String,
        eventType: String,
        targetType: String,
        targetId: String,
        now: Long
    ) {
        SecurityAuditLog.insert {
            it[SecurityAuditLog.id] = UUID.randomUUID().toString()
            it[SecurityAuditLog.actorAccountId] = actorAccountId
            it[SecurityAuditLog.eventType] = eventType
            it[SecurityAuditLog.targetType] = targetType
            it[SecurityAuditLog.targetId] = targetId
            it[SecurityAuditLog.occurredAt] = now
        }
    }
}
