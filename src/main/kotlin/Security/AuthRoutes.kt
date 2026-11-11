package com.est.Security

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

@Serializable
data class AdminBootstrapRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val tokenType: String,
    val expiresAt: Long,
    val accountId: String,
    val email: String,
    val roles: List<String>
)

@Serializable
data class CurrentAccountResponse(
    val accountId: String,
    val email: String,
    val roles: List<String>
)

fun Application.configureAuthRoutes(database: R2dbcDatabase) {
    val appEnvironment = System.getenv("APP_ENV")
        ?.trim()
        ?.lowercase()
        ?: "development"
    val bootstrapService = AdminBootstrapService(
        database = database,
        configuredToken = System.getenv("ADMIN_BOOTSTRAP_TOKEN")
    )
    val sessionService = SessionService(database)

    routing {
        post("/auth/bootstrap") {
            // Keep bootstrap local-only until full route authorization and
            // production HTTPS/proxy configuration have been validated.
            if (appEnvironment != "development") {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Bootstrap indisponível."))
                return@post
            }

            val providedToken = bearerToken(call.request.headers["Authorization"])
            val request = try {
                call.receive<AdminBootstrapRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Pedido inválido."))
                return@post
            }

            val result = try {
                bootstrapService.createFirstAdministrator(
                    providedToken = providedToken,
                    email = request.email,
                    password = request.password
                )
            } catch (_: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Não foi possível concluir o bootstrap."))
                return@post
            }

            when (result) {
                BootstrapResult.CREATED ->
                    call.respond(HttpStatusCode.Created, mapOf("status" to "Administrador criado."))
                BootstrapResult.UNAVAILABLE ->
                    call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Bootstrap indisponível."))
                BootstrapResult.INVALID_TOKEN ->
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Credenciais inválidas."))
                BootstrapResult.INVALID_INPUT ->
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Dados inválidos."))
                BootstrapResult.ALREADY_COMPLETED ->
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Bootstrap já concluído."))
            }
        }

        post("/auth/login") {
            val request = try {
                call.receive<LoginRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Pedido inválido."))
                return@post
            }

            val login = try {
                sessionService.login(request.email, request.password)
            } catch (_: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Não foi possível iniciar sessão."))
                return@post
            }

            val session = login.second
            if (login.first != LoginResult.AUTHENTICATED || session == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Email ou palavra-passe inválidos."))
                return@post
            }

            call.respond(
                HttpStatusCode.OK,
                LoginResponse(
                    token = session.token,
                    tokenType = "Bearer",
                    expiresAt = session.expiresAt,
                    accountId = session.account.accountId,
                    email = session.account.email,
                    roles = session.account.roles.sorted()
                )
            )
        }

        get("/auth/me") {
            val principal = sessionService.authenticate(
                bearerToken(call.request.headers["Authorization"])
            )
            if (principal == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Sessão inválida ou expirada."))
                return@get
            }

            call.respond(
                CurrentAccountResponse(
                    accountId = principal.accountId,
                    email = principal.email,
                    roles = principal.roles.sorted()
                )
            )
        }

        post("/auth/logout") {
            val token = bearerToken(call.request.headers["Authorization"])
            if (token == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Sessão inválida."))
                return@post
            }

            val revoked = try {
                sessionService.logout(token)
            } catch (_: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Não foi possível terminar a sessão."))
                return@post
            }

            if (!revoked) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Sessão inválida ou já terminada."))
                return@post
            }

            call.respond(HttpStatusCode.OK, mapOf("status" to "Sessão terminada."))
        }
    }
}

private fun bearerToken(header: String?): String? =
    header
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substringAfter(' ')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
