package com.est.Classificacao

import com.est.DBUtils.DBUtils
import com.est.DBUtils.DatabaseCreation
import com.est.Jogo.JogoService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.Classificacoes_Rotas() {
    // Usamos o serviço de classificação injetando o JogoService
    val JogoService = JogoService(DBUtils.database)
    val classificacaoService = ClassificacaoService(JogoService)

    routing {
        route("/classificacoes") {

            // GET /classificacao/geral - Agregado de todos os eventos competitivos
            get("/geral") {
                try {
                    val todosJogos = DatabaseCreation.JogoService.lerJogos()
                    val classificacao = classificacaoService.calcularClassificacao(todosJogos)
                    call.respond(HttpStatusCode.OK, classificacao)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Erro ao calcular classificação geral: ${e.message}")
                }
            }

            // GET /classificacao/torneio/{eventoId} - Classificação específica para um torneio
            get("/torneio/{eventoId}") {
                try {
                    val eventoId = call.parameters["eventoId"]?.toIntOrNull()
                    if (eventoId == null) {
                        call.respond(HttpStatusCode.BadRequest, "ID de evento inválido")
                        return@get
                    }

                    val jogosDoEvento = DatabaseCreation.JogoService.lerJogosPorEvento(eventoId)
                    val classificacao = classificacaoService.calcularClassificacao(jogosDoEvento)
                    call.respond(HttpStatusCode.OK, classificacao)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Erro ao calcular classificação do torneio: ${e.message}")
                }
            }
        }
    }
}