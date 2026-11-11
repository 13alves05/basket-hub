package com.est.Security

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

@Serializable
data class AdminBootstrapRequest(
    val email: String,
    val password: String
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

    routing {
        post("/auth/bootstrap") {
            // Keep the bootstrap endpoint local-only until full route authorization
            // and production HTTPS/proxy configuration have been validated.
            if (appEnvironment != "development") {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Bootstrap indisponível."))
                return@post
            }

            val authorization = call.request.headers["Authorization"]
            val providedToken = authorization
                ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
                ?.substringAfter(' ')
                ?.trim()

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
    }
}
