package com.est.Security

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.intercept

/**
 * Temporary fail-closed policy for legacy business routes.
 *
 * Public reads are explicitly allowlisted. Every other request handled by a
 * route group using this guard requires an active Administrator session.
 * Role-specific scopes will replace this conservative policy as each domain
 * relationship is validated. Do not remove the production startup barrier.
 */
fun Route.enforceTemporaryAdministratorPolicy(sessionService: SessionService) {
    intercept(ApplicationCallPipeline.Call) {
        val method = call.request.httpMethod
        val path = call.request.path()

        if (isPublicRead(method, path) || isDelegatedScopedWrite(method, path)) {
            proceed()
            return@intercept
        }

        val token = call.request.headers["Authorization"]
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter(' ')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val account = sessionService.authenticate(token)
        when {
            account == null -> {
                call.respondText("Autenticação necessária.", status = HttpStatusCode.Unauthorized)
                finish()
            }
            "ADMINISTRADOR" !in account.roles -> {
                call.respondText("Acesso reservado ao Administrador até à conclusão da autorização por âmbito.", status = HttpStatusCode.Forbidden)
                finish()
            }
            else -> proceed()
        }
    }
}

/**
 * These writes are exempt from the temporary Administrator-only guard because
 * their endpoint performs an explicit role-and-resource-scope authorization check.
 */
internal fun isDelegatedScopedWrite(method: HttpMethod, path: String): Boolean =
    method == HttpMethod.Post && path.matches(Regex("^/eventos/\\\\d+/agendamento$"))

internal fun isPublicRead(method: HttpMethod, path: String): Boolean {
    if (method != HttpMethod.Get) return false

    return path == "/" ||
        path == "/hub" ||
        path == "/publico" ||
        path == "/eventos" ||
        path.matches(Regex("^/eventos/\\d+$")) ||
        path.matches(Regex("^/eventos/\\d+/chaveamento$")) ||
        path == "/classificacoes/geral" ||
        path.matches(Regex("^/classificacoes/torneio/\\d+$")) ||
        path == "/json/kotlinx-serialization" ||
        path == "/test" ||
        path.startsWith("/static/")
}
