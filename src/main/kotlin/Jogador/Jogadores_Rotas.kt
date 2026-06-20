package com.est.Jogador

import com.est.DBUtils.DBUtils
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.Jogadores_Rotas() {

    val jogadorService = JogadorService(DBUtils.database)

    routing {

        // adicionar jogador
        post("/jogadores") {
            try {
                val jogador = call.receive<Jogador>()

                jogadorService.criarJogador(jogador)

                call.respond(HttpStatusCode.Created, "Jogador criado")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Erro")
            }
        }

        // buscar todos os jogadores
        get("/jogadores") {
            try {
                val lista = jogadorService.lerJogadores()

                call.respond(HttpStatusCode.OK, lista)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }

        // atualizar jogador
        put("/jogadores/{id}") {
            val id = call.parameters["id"]!!.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
            val jogador = call.receive<Jogador>()
            jogadorService.atualizarJogador(id, jogador)
            call.respond(HttpStatusCode.OK, "Jogador atualizado")
        }

        // apagar jogador
        delete("/jogadores/{id}") {
            val id = call.parameters["id"]!!.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            jogadorService.removerJogador(id)
            call.respond(HttpStatusCode.OK, "Jogador removido")
        }

        // ler jogador por id
        get("/jogadores/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID invalido")
                    return@get
                }

                val jogador = jogadorService.lerJogadorPorId(id)

                if (jogador != null) {
                    call.respond(HttpStatusCode.OK, jogador)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Jogador não encontrado")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao ler jogador")
            }
        }
    }
}