package com.est.Jogo

import com.est.Security.AuthorizationService
import com.est.Security.SessionService
import com.est.DBUtils.DBUtils
import com.est.Evento.EventoService
import com.est.Inscricao.InscricaoService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

private const val ORGANIZER_ROLE = "ORGANIZADOR"
private const val EVENT_SCOPE = "EVENTO"

private suspend fun ApplicationCall.gameAccount(sessions: SessionService) =
    request.headers["Authorization"]
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substringAfter(' ')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { sessions.authenticate(it) }

fun Application.Jogos_Rotas() {
    val service = JogoService(DBUtils.database)
    val sessions = SessionService(DBUtils.database)
    val authorization = AuthorizationService(DBUtils.database)
    val eventos = EventoService(DBUtils.database)
    val inscricoes = InscricaoService(DBUtils.database)


    suspend fun validGameForEvent(game: Jogo): Boolean {
        val event = eventos.lerEventoPorId(game.eventoId) ?: return false
        if (game.pavilhaoId != event.pavilhaoId) return false
        val registeredTeams = inscricoes.lerInscricoes()
            .filter { it.eventoId == game.eventoId }
            .map { it.equipaId }
            .toSet()
        if (game.equipaCasaId !in registeredTeams) return false
        if (game.equipaForaId != null && game.equipaForaId !in registeredTeams) return false
        if (game.equipaForaId != null && game.equipaForaId == game.equipaCasaId) return false
        return true
    }

    routing {
        route("/jogos") {
            post {
                val account = call.gameAccount(sessions)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                    return@post
                }
                try {
                    val dados = call.receive<Jogo>()
                    if ("ADMINISTRADOR" !in account.roles &&
                        !authorization.canAccess(account.accountId, ORGANIZER_ROLE, EVENT_SCOPE, dados.eventoId.toString())
                    ) {
                        call.respond(HttpStatusCode.Forbidden, "Sem acesso ao evento deste jogo")
                        return@post
                    }
                    if (!validGameForEvent(dados)) {
                        call.respond(HttpStatusCode.BadRequest, "O jogo tem de usar o pavilhão e equipas inscritos no evento indicado")
                        return@post
                    }
                    service.criarJogo(dados)
                    call.respond(HttpStatusCode.Created, "Jogo criado com sucesso")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Erro ao criar jogo")
                }
            }

            get {
                val account = call.gameAccount(sessions)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                    return@get
                }
                if ("ADMINISTRADOR" in account.roles) {
                    call.respond(HttpStatusCode.OK, service.lerJogos())
                    return@get
                }
                if (ORGANIZER_ROLE !in account.roles) {
                    call.respond(HttpStatusCode.Forbidden, "Acesso reservado ao Organizador")
                    return@get
                }
                val eventIds = authorization.accessibleScopeIds(
                    account.accountId, ORGANIZER_ROLE, EVENT_SCOPE
                ).orEmpty()
                val jogos = service.lerJogos().filter { it.eventoId.toString() in eventIds }
                call.respond(HttpStatusCode.OK, jogos)
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID inválido")
                    return@get
                }
                val account = call.gameAccount(sessions)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                    return@get
                }
                val jogo = service.lerJogoPorId(id)
                if (jogo == null) {
                    call.respond(HttpStatusCode.NotFound, "Jogo não encontrado")
                    return@get
                }
                if ("ADMINISTRADOR" !in account.roles &&
                    !authorization.canAccess(account.accountId, ORGANIZER_ROLE, EVENT_SCOPE, jogo.eventoId.toString())
                ) {
                    call.respond(HttpStatusCode.Forbidden, "Sem acesso ao evento deste jogo")
                    return@get
                }
                call.respond(HttpStatusCode.OK, jogo)
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID inválido")
                    return@put
                }
                val account = call.gameAccount(sessions)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                    return@put
                }
                val atual = service.lerJogoPorId(id)
                if (atual == null) {
                    call.respond(HttpStatusCode.NotFound, "Jogo não encontrado")
                    return@put
                }
                if ("ADMINISTRADOR" !in account.roles &&
                    !authorization.canAccess(account.accountId, ORGANIZER_ROLE, EVENT_SCOPE, atual.eventoId.toString())
                ) {
                    call.respond(HttpStatusCode.Forbidden, "Sem acesso ao evento deste jogo")
                    return@put
                }
                try {
                    val dados = call.receive<Jogo>()
                    if (dados.eventoId != atual.eventoId) {
                        call.respond(HttpStatusCode.Forbidden, "Não é permitido transferir um jogo para outro evento")
                        return@put
                    }
                    if (!validGameForEvent(dados)) {
                        call.respond(HttpStatusCode.BadRequest, "O jogo tem de usar o pavilhão e equipas inscritos no evento indicado")
                        return@put
                    }
                    service.atualizarJogo(id, dados)
                    call.respond(HttpStatusCode.OK, "Jogo atualizado com sucesso")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Erro ao atualizar jogo")
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID inválido")
                    return@delete
                }
                val account = call.gameAccount(sessions)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Autenticação necessária")
                    return@delete
                }
                val jogo = service.lerJogoPorId(id)
                if (jogo == null) {
                    call.respond(HttpStatusCode.NotFound, "Jogo não encontrado")
                    return@delete
                }
                if ("ADMINISTRADOR" !in account.roles &&
                    !authorization.canAccess(account.accountId, ORGANIZER_ROLE, EVENT_SCOPE, jogo.eventoId.toString())
                ) {
                    call.respond(HttpStatusCode.Forbidden, "Sem acesso ao evento deste jogo")
                    return@delete
                }
                service.removerJogo(id)
                call.respond(HttpStatusCode.OK, "Jogo removido com sucesso")
            }
        }
    }
}
