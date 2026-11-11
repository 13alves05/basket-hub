package com.est.Security

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * Resource-scoped authorization primitive.
 *
 * A non-administrator must hold the requested role AND an explicit matching
 * resource scope for that same role. Roles are never combined across scopes.
 */
class AuthorizationService(private val database: R2dbcDatabase) {
    suspend fun canAccess(
        accountId: String,
        requiredRole: String,
        scopeType: String,
        scopeId: String
    ): Boolean = suspendTransaction(database) {
        val accountIsActive = Accounts
            .selectAll()
            .where { Accounts.id eq accountId }
            .limit(1)
            .map { it[Accounts.isActive] }
            .singleOrNull() == true

        if (!accountIsActive) {
            return@suspendTransaction false
        }

        val hasAdministratorRole = AccountRoles
            .selectAll()
            .where {
                (AccountRoles.accountId eq accountId) and
                    (AccountRoles.roleCode eq "ADMINISTRADOR")
            }
            .limit(1)
            .map { it[AccountRoles.roleCode] }
            .singleOrNull() != null

        if (hasAdministratorRole) {
            return@suspendTransaction true
        }

        val hasRequiredRole = AccountRoles
            .selectAll()
            .where {
                (AccountRoles.accountId eq accountId) and
                    (AccountRoles.roleCode eq requiredRole)
            }
            .limit(1)
            .map { it[AccountRoles.roleCode] }
            .singleOrNull() != null

        if (!hasRequiredRole) {
            return@suspendTransaction false
        }

        RoleScopes
            .selectAll()
            .where {
                (RoleScopes.accountId eq accountId) and
                    (RoleScopes.roleCode eq requiredRole) and
                    (RoleScopes.scopeType eq scopeType) and
                    (RoleScopes.scopeId eq scopeId)
            }
            .limit(1)
            .map { it[RoleScopes.id] }
            .singleOrNull() != null
    }

    suspend fun hasGlobalRole(accountId: String, roleCode: String): Boolean =
        suspendTransaction(database) {
            AccountRoles
                .selectAll()
                .where {
                    (AccountRoles.accountId eq accountId) and
                        (AccountRoles.roleCode eq roleCode)
                }
                .limit(1)
                .map { it[AccountRoles.roleCode] }
                .singleOrNull() != null
        }

    /**
     * Returns null for an Administrator (global access), an empty set when
     * the account has no matching scopes, or the exact assigned scope IDs.
     */
    suspend fun accessibleScopeIds(
        accountId: String,
        requiredRole: String,
        scopeType: String
    ): Set<String>? = suspendTransaction(database) {
        val active = Accounts
            .selectAll()
            .where { (Accounts.id eq accountId) and (Accounts.isActive eq true) }
            .limit(1)
            .map { it[Accounts.id] }
            .singleOrNull() != null
        if (!active) return@suspendTransaction emptySet()

        val isAdministrator = AccountRoles
            .selectAll()
            .where {
                (AccountRoles.accountId eq accountId) and
                    (AccountRoles.roleCode eq "ADMINISTRADOR")
            }
            .limit(1)
            .map { it[AccountRoles.roleCode] }
            .singleOrNull() != null
        if (isAdministrator) return@suspendTransaction null

        val hasRole = AccountRoles
            .selectAll()
            .where {
                (AccountRoles.accountId eq accountId) and
                    (AccountRoles.roleCode eq requiredRole)
            }
            .limit(1)
            .map { it[AccountRoles.roleCode] }
            .singleOrNull() != null
        if (!hasRole) return@suspendTransaction emptySet()

        RoleScopes
            .selectAll()
            .where {
                (RoleScopes.accountId eq accountId) and
                    (RoleScopes.roleCode eq requiredRole) and
                    (RoleScopes.scopeType eq scopeType)
            }
            .map { it[RoleScopes.scopeId] }
            .toList()
            .toSet()
    }
}
