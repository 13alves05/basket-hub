package com.est.Security

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

enum class BootstrapResult {
    CREATED,
    UNAVAILABLE,
    INVALID_TOKEN,
    INVALID_INPUT,
    ALREADY_COMPLETED
}

class AdminBootstrapService(
    private val database: R2dbcDatabase,
    private val configuredToken: String?
) {
    companion object {
        private const val BOOTSTRAP_STATE_KEY = "first_admin_bootstrap"
        private const val ADMIN_ROLE = "ADMINISTRADOR"
        private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }

    suspend fun createFirstAdministrator(
        providedToken: String?,
        email: String,
        password: String
    ): BootstrapResult {
        val expectedToken = configuredToken
            ?.takeIf { it.toByteArray(Charsets.UTF_8).size >= 32 }
            ?: return BootstrapResult.UNAVAILABLE

        if (providedToken.isNullOrBlank() ||
            !MessageDigest.isEqual(
                expectedToken.toByteArray(Charsets.UTF_8),
                providedToken.toByteArray(Charsets.UTF_8)
            )
        ) {
            return BootstrapResult.INVALID_TOKEN
        }

        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        if (!EMAIL_PATTERN.matches(normalizedEmail) || password.length < 12) {
            return BootstrapResult.INVALID_INPUT
        }

        val passwordHash = PasswordHasher.hash(password)
        val accountId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        return try {
            suspendTransaction(database) {
                // This primary-key insert is the concurrency gate. Only one
                // transaction can claim the one-time bootstrap state.
                SystemSecurityState.insert {
                    it[stateKey] = BOOTSTRAP_STATE_KEY
                    it[stateValue] = "completed"
                    it[updatedAt] = now
                }

                val existingAdministrator = AccountRoles
                    .selectAll()
                    .where { AccountRoles.roleCode eq ADMIN_ROLE }
                    .limit(1)
                    .map { it[AccountRoles.roleCode] }
                    .singleOrNull()

                if (existingAdministrator != null) {
                    throw ExistingAdministratorException()
                }

                Accounts.insert {
                    it[Accounts.id] = accountId
                    it[Accounts.emailNormalized] = normalizedEmail
                    it[Accounts.passwordHash] = passwordHash
                    it[Accounts.isActive] = true
                    it[Accounts.createdAt] = now
                    it[Accounts.updatedAt] = now
                }

                AccountRoles.insert {
                    it[AccountRoles.accountId] = accountId
                    it[AccountRoles.roleCode] = ADMIN_ROLE
                    it[AccountRoles.assignedAt] = now
                }

                SecurityAuditLog.insert {
                    it[SecurityAuditLog.id] = UUID.randomUUID().toString()
                    it[SecurityAuditLog.actorAccountId] = null
                    it[SecurityAuditLog.eventType] = "FIRST_ADMIN_BOOTSTRAP_COMPLETED"
                    it[SecurityAuditLog.targetType] = "ACCOUNT"
                    it[SecurityAuditLog.targetId] = accountId
                    it[SecurityAuditLog.occurredAt] = now
                }
            }
            BootstrapResult.CREATED
        } catch (_: ExistingAdministratorException) {
            BootstrapResult.ALREADY_COMPLETED
        } catch (_: Exception) {
            // A concurrent request can lose the unique bootstrap-state insert.
            // Re-check after rollback so a completed bootstrap returns a
            // stable conflict result without leaking database details.
            val alreadyCompleted = suspendTransaction(database) {
                SystemSecurityState
                    .selectAll()
                    .where { SystemSecurityState.stateKey eq BOOTSTRAP_STATE_KEY }
                    .limit(1)
                    .map { it[SystemSecurityState.stateKey] }
                    .singleOrNull() != null
            }
            if (alreadyCompleted) BootstrapResult.ALREADY_COMPLETED else throw BootstrapStorageException()
        }
    }

    private class ExistingAdministratorException : RuntimeException()
    private class BootstrapStorageException : RuntimeException()
}
