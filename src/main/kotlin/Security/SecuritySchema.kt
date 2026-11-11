package com.est.Security

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * Persistent identity and authorization schema.
 *
 * This file deliberately defines storage only. No route is considered protected
 * until authentication and resource-scope authorization are implemented.
 */
object Accounts : Table("accounts") {
    val id = varchar("id", 36)
    val emailNormalized = varchar("email_normalized", 320)
    val passwordHash = varchar("password_hash", 512)
    val isActive = bool("is_active").default(true)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object Roles : Table("roles") {
    val code = varchar("code", 40)
    val description = varchar("description", 160)

    override val primaryKey = PrimaryKey(code)
}

object AccountRoles : Table("account_roles") {
    val accountId = reference("account_id", Accounts.id)
    val roleCode = reference("role_code", Roles.code)
    val assignedAt = long("assigned_at")

    override val primaryKey = PrimaryKey(accountId, roleCode)
}

object RoleScopes : Table("role_scopes") {
    val id = varchar("id", 36)
    val accountId = reference("account_id", Accounts.id)
    val roleCode = reference("role_code", Roles.code)
    val scopeType = varchar("scope_type", 32)
    val scopeId = varchar("scope_id", 100)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object AuthSessions : Table("auth_sessions") {
    val id = varchar("id", 36)
    val accountId = reference("account_id", Accounts.id)
    val tokenHash = varchar("token_hash", 128)
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")
    val revokedAt = long("revoked_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object SystemSecurityState : Table("system_security_state") {
    val stateKey = varchar("state_key", 80)
    val stateValue = varchar("state_value", 200)
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(stateKey)
}

object SecurityAuditLog : Table("security_audit_log") {
    val id = varchar("id", 36)
    val actorAccountId = reference("actor_account_id", Accounts.id).nullable()
    val eventType = varchar("event_type", 80)
    val targetType = varchar("target_type", 80).nullable()
    val targetId = varchar("target_id", 100).nullable()
    val occurredAt = long("occurred_at")

    override val primaryKey = PrimaryKey(id)
}

object SecuritySchema {
    suspend fun create(database: R2dbcDatabase) {
        suspendTransaction(database) {
            SchemaUtils.create(
                Accounts,
                Roles,
                AccountRoles,
                RoleScopes,
                AuthSessions,
                SystemSecurityState,
                SecurityAuditLog
            )
        }
    }
}
