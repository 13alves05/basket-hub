package com.est.Jogo

import com.est.DBUtils.DBUtils
import com.est.Jogo.Jogo

import com.est.DBUtils.DatabaseCreation
import com.est.Jogo.JogoService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.Jogos_Rotas() {

    val service = JogoService(DBUtils.database)

    routing {
        route("/jogos") {
            post {
                try {
                    val dados = call.receive<Jogo>()
                    service.criarJogo(dados)
                    call.respond(HttpStatusCode.Created, "Jogo criado com sucesso")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Erro ao criar jogo")
                }
            }

            get {
                try {
                    val lista = service.lerJogos()
                    call.respond(HttpStatusCode.OK, lista)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao ler jogos")
                }
            }

            get("/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, "ID invalido")
                        return@get
                    }
                    val jogo = service.lerJogoPorId(id)
                    if (jogo != null) {
                        call.respond(HttpStatusCode.OK, jogo)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Jogo nao encontrado")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao buscar jogo")
                }
            }

            put("/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, "ID invalido")
                        return@put
                    }
                    val dados = call.receive<Jogo>()
                    service.atualizarJogo(id, dados)
                    call.respond(HttpStatusCode.OK, "Jogo atualizado com sucesso")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao atualizar jogo")
                }
            }

            delete("/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, "ID invalido")
                        return@delete
                    }
                    service.removerJogo(id)
                    call.respond(HttpStatusCode.OK, "Jogo removido com sucesso")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao remover jogo")
                }
            }
        }
    }
}
