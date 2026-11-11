package com.est.Evento

import com.est.Security.AuthorizationService
import com.est.Security.SessionService
import com.est.DBUtils.DBUtils
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

private const val ORGANIZER_ROLE = "ORGANIZADOR"
private const val EVENT_SCOPE = "EVENTO"

private suspend fun ApplicationCall.eventAccount(sessions: SessionService) =
    request.headers["Authorization"]
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substringAfter(' ')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { sessions.authenticate(it) }

fun Application.Eventos_Rotas() {
    val service = EventoService(DBUtils.database)
    val sessions = SessionService(DBUtils.database)
    val authorization = AuthorizationService(DBUtils.database)

    routing {
        route("/eventos") {
            // A criação de eventos altera o domínio global e fica reservada ao Administrador.
            post {
                val account = call.eventAccount(sessions)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                    return@post
                }
                if ("ADMINISTRADOR" !in account.roles) {
                    call.respond(HttpStatusCode.Forbidden, "Apenas o Administrador pode criar eventos")
                    return@post
                }
                try {
                    val dados = call.receive<Evento>()
                    service.criarEvento(dados)
                    call.respond(HttpStatusCode.Created, "Evento criado com sucesso")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Erro ao criar evento")
                }
            }

            // A consulta pública dos eventos mantém-se disponível.
            get {
                try {
                    call.respond(HttpStatusCode.OK, service.lerEventos())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao ler eventos")
                }
            }

            get("/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, "ID inválido")
                        return@get
                    }
                    val evento = service.lerEventoPorId(id)
                    if (evento != null) call.respond(HttpStatusCode.OK, evento)
                    else call.respond(HttpStatusCode.NotFound, "Evento não encontrado")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao ler evento")
                }
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID inválido")
                    return@put
                }
                val account = call.eventAccount(sessions)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                    return@put
                }
                val current = service.lerEventoPorId(id)
                if (current == null) {
                    call.respond(HttpStatusCode.NotFound, "Evento não encontrado")
                    return@put
                }
                if (!authorization.canAccess(
                        account.accountId, ORGANIZER_ROLE, EVENT_SCOPE, id.toString()
                    )
                ) {
                    call.respond(HttpStatusCode.Forbidden, "Sem acesso a este evento")
                    return@put
                }
                try {
                    val dados = call.receive<Evento>()
                    // Um Organizador com âmbito neste evento não pode transferir a sua
                    // associação para outro Organizador através do payload de atualização.
                    if ("ADMINISTRADOR" !in account.roles &&
                        dados.organizadorId != current.organizadorId
                    ) {
                        call.respond(HttpStatusCode.Forbidden, "Não pode alterar o Organizador responsável")
                        return@put
                    }
                    service.atualizarEvento(id, dados)
                    call.respond(HttpStatusCode.OK, "Evento atualizado")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Erro ao atualizar evento")
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID inválido")
                    return@delete
                }
                val account = call.eventAccount(sessions)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                    return@delete
                }
                if (!authorization.canAccess(
                        account.accountId, ORGANIZER_ROLE, EVENT_SCOPE, id.toString()
                    )
                ) {
                    call.respond(HttpStatusCode.Forbidden, "Sem acesso a este evento")
                    return@delete
                }
                try {
                    val current = service.lerEventoPorId(id)
                    if (current == null) {
                        call.respond(HttpStatusCode.NotFound, "Evento não encontrado")
                        return@delete
                    }
                    service.removerEvento(id)
                    call.respond(HttpStatusCode.OK, "Evento removido")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao remover evento")
                }
            }
        }
    }
}