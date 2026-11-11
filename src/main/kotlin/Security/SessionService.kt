package com.est.Security

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import java.util.UUID

data class AuthenticatedAccount(
    val accountId: String,
    val email: String,
    val roles: Set<String>
)

data class IssuedSession(
    val token: String,
    val expiresAt: Long,
    val account: AuthenticatedAccount
)

enum class LoginResult {
    AUTHENTICATED,
    INVALID_CREDENTIALS
}

/**
 * Bearer sessions are opaque random tokens. Only their SHA-256 digest is stored.
 * The raw token is returned once at login and must not be logged or persisted by
 * the server in plaintext.
 */
class SessionService(
    private val database: R2dbcDatabase,
    private val sessionLifetimeMillis: Long = 8 * 60 * 60 * 1000L
) {
    companion object {
        private const val TOKEN_BYTES = 32
        private val secureRandom = SecureRandom()
    }

    suspend fun login(email: String, password: String): Pair<LoginResult, IssuedSession?> {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val account = suspendTransaction(database) {
            Accounts
                .selectAll()
                .where { Accounts.emailNormalized eq normalizedEmail }
                .limit(1)
                .map {
                    Triple(
                        it[Accounts.id],
                        it[Accounts.passwordHash],
                        it[Accounts.isActive]
                    )
                }
                .singleOrNull()
        } ?: return LoginResult.INVALID_CREDENTIALS to null

        if (!account.third || !PasswordHasher.verify(password, account.second)) {
            return LoginResult.INVALID_CREDENTIALS to null
        }

        val rawToken = ByteArray(TOKEN_BYTES).also(secureRandom::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(rawToken)
        rawToken.fill(0)
        val tokenHash = sha256(token)
        val now = System.currentTimeMillis()
        val expiresAt = now + sessionLifetimeMillis
        val sessionId = UUID.randomUUID().toString()

        val roles = suspendTransaction(database) {
            AccountRoles
                .selectAll()
                .where { AccountRoles.accountId eq account.first }
                .map { it[AccountRoles.roleCode] }
                .toList()
                .toSet()
        }

        suspendTransaction(database) {
            AuthSessions.insert {
                it[AuthSessions.id] = sessionId
                it[AuthSessions.accountId] = account.first
                it[AuthSessions.tokenHash] = tokenHash
                it[AuthSessions.createdAt] = now
                it[AuthSessions.expiresAt] = expiresAt
                it[AuthSessions.revokedAt] = null
            }

            SecurityAuditLog.insert {
                it[SecurityAuditLog.id] = UUID.randomUUID().toString()
                it[SecurityAuditLog.actorAccountId] = account.first
                it[SecurityAuditLog.eventType] = "AUTH_LOGIN_SUCCEEDED"
                it[SecurityAuditLog.targetType] = "SESSION"
                it[SecurityAuditLog.targetId] = sessionId
                it[SecurityAuditLog.occurredAt] = now
            }
        }

        val principal = AuthenticatedAccount(account.first, normalizedEmail, roles)
        return LoginResult.AUTHENTICATED to IssuedSession(token, expiresAt, principal)
    }

    suspend fun authenticate(token: String?): AuthenticatedAccount? {
        if (token.isNullOrBlank() || token.length > 256) return null
        val tokenHash = sha256(token)
        val now = System.currentTimeMillis()

        val session = suspendTransaction(database) {
            AuthSessions
                .selectAll()
                .where { AuthSessions.tokenHash eq tokenHash }
                .limit(1)
                .map {
                    Triple(
                        it[AuthSessions.accountId],
                        it[AuthSessions.expiresAt],
                        it[AuthSessions.revokedAt]
                    )
                }
                .singleOrNull()
        } ?: return null

        if (session.second <= now || session.third != null) return null

        val account = suspendTransaction(database) {
            Accounts
                .selectAll()
                .where { Accounts.id eq session.first }
                .limit(1)
                .map {
                    Triple(
                        it[Accounts.id],
                        it[Accounts.emailNormalized],
                        it[Accounts.isActive]
                    )
                }
                .singleOrNull()
        } ?: return null

        if (!account.third) return null

        val roles = suspendTransaction(database) {
            AccountRoles
                .selectAll()
                .where { AccountRoles.accountId eq account.first }
                .map { it[AccountRoles.roleCode] }
                .toList()
                .toSet()
        }

        return AuthenticatedAccount(account.first, account.second, roles)
    }

    suspend fun logout(token: String?): Boolean {
        if (token.isNullOrBlank() || token.length > 256) return false
        val tokenHash = sha256(token)
        val now = System.currentTimeMillis()

        return suspendTransaction(database) {
            val sessionId = AuthSessions
                .selectAll()
                .where {
                    (AuthSessions.tokenHash eq tokenHash) and
                        AuthSessions.revokedAt.isNull()
                }
                .limit(1)
                .map { it[AuthSessions.id] }
                .singleOrNull()
                ?: return@suspendTransaction false

            AuthSessions.update({ AuthSessions.id eq sessionId }) {
                it[AuthSessions.revokedAt] = now
            }

            SecurityAuditLog.insert {
                it[SecurityAuditLog.id] = UUID.randomUUID().toString()
                it[SecurityAuditLog.actorAccountId] = null
                it[SecurityAuditLog.eventType] = "AUTH_SESSION_REVOKED"
                it[SecurityAuditLog.targetType] = "SESSION"
                it[SecurityAuditLog.targetId] = sessionId
                it[SecurityAuditLog.occurredAt] = now
            }
            true
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
