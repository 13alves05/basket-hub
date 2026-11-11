package com.est.Equipa

import com.est.Security.AuthorizationService
import com.est.Security.AuthenticatedAccount
import com.est.Security.SessionService
import com.est.DBUtils.DBUtils
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

private const val TEAM_MANAGER_ROLE = "GESTOR_EQUIPA"
private const val TEAM_SCOPE_TYPE = "EQUIPA"

private suspend fun ApplicationCall.authenticatedAccount(
    sessions: SessionService
): AuthenticatedAccount? {
    val token = request.headers["Authorization"]
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substringAfter(' ')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    return sessions.authenticate(token)
}

fun Application.Equipas_Rotas() {
    val equipaService = EquipaService(DBUtils.database)
    val sessions = SessionService(DBUtils.database)
    val authorization = AuthorizationService(DBUtils.database)

    routing {
        post("/equipas") {
            val account = call.authenticatedAccount(sessions)
            if (account == null) {
                call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                return@post
            }
            if ("ADMINISTRADOR" !in account.roles) {
                call.respond(HttpStatusCode.Forbidden, "Apenas o Administrador pode criar equipas")
                return@post
            }
            try {
                val equipa = call.receive<Equipa>()
                equipaService.criarEquipa(equipa)
                call.respond(HttpStatusCode.Created, "Equipa criada")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Erro")
            }
        }

        get("/equipas") {
            val account = call.authenticatedAccount(sessions)
            if (account == null) {
                call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                return@get
            }
            val scopeIds = authorization.accessibleScopeIds(
                account.accountId, TEAM_MANAGER_ROLE, TEAM_SCOPE_TYPE
            )
            if (scopeIds == null || "ADMINISTRADOR" in account.roles) {
                call.respond(HttpStatusCode.OK, equipaService.lerEquipas())
                return@get
            }
            if (TEAM_MANAGER_ROLE !in account.roles) {
                call.respond(HttpStatusCode.Forbidden, "Acesso reservado ao Gestor de Equipa")
                return@get
            }
            val teams = equipaService.lerEquipas().filter { it.id?.toString() in scopeIds }
            call.respond(HttpStatusCode.OK, teams)
        }

        put("/equipas/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
                return@put
            }
            val account = call.authenticatedAccount(sessions)
            if (account == null) {
                call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                return@put
            }
            if (!authorization.canAccess(
                    account.accountId, TEAM_MANAGER_ROLE, TEAM_SCOPE_TYPE, id.toString()
                )
            ) {
                call.respond(HttpStatusCode.Forbidden, "Sem acesso a esta equipa")
                return@put
            }
            val equipa = call.receive<Equipa>()
            equipaService.atualizarEquipa(id, equipa)
            call.respond(HttpStatusCode.OK, "Equipa atualizada")
        }

        delete("/equipas/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
                return@delete
            }
            val account = call.authenticatedAccount(sessions)
            if (account == null) {
                call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                return@delete
            }
            if (!authorization.canAccess(
                    account.accountId, TEAM_MANAGER_ROLE, TEAM_SCOPE_TYPE, id.toString()
                )
            ) {
                call.respond(HttpStatusCode.Forbidden, "Sem acesso a esta equipa")
                return@delete
            }
            equipaService.removerEquipa(id)
            call.respond(HttpStatusCode.OK, "Equipa removida")
        }

        get("/equipas/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
                return@get
            }
            val account = call.authenticatedAccount(sessions)
            if (account == null) {
                call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                return@get
            }
            if (!authorization.canAccess(
                    account.accountId, TEAM_MANAGER_ROLE, TEAM_SCOPE_TYPE, id.toString()
                )
            ) {
                call.respond(HttpStatusCode.Forbidden, "Sem acesso a esta equipa")
                return@get
            }
            val equipa = equipaService.lerEquipaPorId(id)
            if (equipa != null) {
                call.respond(HttpStatusCode.OK, equipa)
            } else {
                call.respond(HttpStatusCode.NotFound, "Equipa não encontrada")
            }
        }
    }
}
