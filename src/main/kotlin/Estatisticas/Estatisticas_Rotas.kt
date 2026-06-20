package com.est.Estatisticas

import com.est.DBUtils.DBUtils
import com.est.DBUtils.DatabaseCreation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

import com.est.Estatisticas.*

fun Application.Estatisticas_Rotas() {
    val service = EstatisticasService(DBUtils.database)

    routing {
        route("/estatisticas") {

            // Criar/Atualizar estado
            post {
                val dados = call.receive<Estatisticas>()
                service.registarMovimento(dados)
                call.respond(HttpStatusCode.Created, "Movimento registado com sucesso")
            }

            // Ler histórico de um jogador
            get("/{jogadorId}") {
                val id = call.parameters["jogadorId"]?.toIntOrNull()
                if (id != null) {
                    val historico = service.lerHistoricoJogador(id)
                    call.respond(HttpStatusCode.OK, historico)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "ID de jogador inválido")
                }
            }
        }
    }
}