package com.est.Security

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/**
 * Shared HTTP boundary for bracket generation. Keeping the route factory
 * injectable lets integration tests exercise the production handler with
 * an isolated database and a harmless generation callback.
 */
fun Route.eventBracketGenerationRoute(
    sessions: SessionService,
    authorization: AuthorizationService,
    generate: suspend (Int) -> String
) {
    post("/eventos/{id}/agendamento") {
        val eventId = call.parameters["id"]?.toIntOrNull()
        if (eventId == null) {
            call.respond(HttpStatusCode.BadRequest, "ID inválido")
            return@post
        }

        val token = call.request.headers[HttpHeaders.Authorization]
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter(' ')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val account = sessions.authenticate(token)
        if (account == null) {
            call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
            return@post
        }

        if ("ADMINISTRADOR" !in account.roles &&
            !authorization.canAccess(account.accountId, "ORGANIZADOR", "EVENTO", eventId.toString())
        ) {
            call.respond(HttpStatusCode.Forbidden, "Sem acesso ao evento deste agendamento")
            return@post
        }

        try {
            call.respond(HttpStatusCode.OK, generate(eventId))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.NotFound, e.message ?: "Evento não encontrado")
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, e.message ?: "Não foi possível avançar o chaveamento")
        }
    }
}
