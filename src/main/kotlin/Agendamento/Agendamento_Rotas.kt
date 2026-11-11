package com.est.Agendamento

import com.est.Security.SessionService
import com.est.Security.enforceTemporaryAdministratorPolicy
import com.est.DBUtils.DBUtils
import com.est.DBUtils.DatabaseCreation
import com.est.Evento.EventoService
import com.est.Inscricao.InscricaoService
import com.est.Jogo.JogoService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.Agendamento_Rotas() {

    val EventoService = EventoService(DBUtils.database)
    val InscricaoService = InscricaoService(DBUtils.database)
    val JogoService = JogoService(DBUtils.database)

    val service = AgendamentoService(
        EventoService,
        InscricaoService,
        JogoService
    )

    routing {
        enforceTemporaryAdministratorPolicy(SessionService(DBUtils.database))
        // Consulta todos os jogos (todas as rondas) já gerados para um evento
        get("/eventos/{id}/chaveamento") {
            try {
                val eventoId = call.parameters["id"]?.toIntOrNull()
                if (eventoId == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID de evento inválido")
                    return@get
                }
                call.respond(HttpStatusCode.OK, JogoService.lerJogosPorEvento(eventoId))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao ler chaveamento")
            }
        }
    }
}
