package com.est.Pavilhao

import com.est.DBUtils.DBUtils
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.Pavilhoes_Rotas() {
    val PavilhaoService = PavilhaoService(DBUtils.database)

    routing {
        route("/pavilhoes") {
            post {
                try {
                    PavilhaoService.criarPavilhao(call.receive<Pavilhao>())
                    call.respond(HttpStatusCode.Created, "Pavilhão criado com sucesso")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Erro ao criar Pavilhao")
                }
            }
            get {
                call.respond(HttpStatusCode.OK, PavilhaoService.lerTodos())
            }
        }

        //
        put("/pavilhoes/{id}") {
            val id = call.parameters["id"]!!.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
            val pavilhao = call.receive<Pavilhao>()
            PavilhaoService.atualizarPavilhao(id, pavilhao)
            call.respond(HttpStatusCode.OK, "Pavilhão atualizado")
        }

        delete("/pavilhoes/{id}") {
            val id = call.parameters["id"]!!.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            PavilhaoService.removerPavilhao(id)
            call.respond(HttpStatusCode.OK, "Pavilhão eliminado")
        }

        get("/pavilhoes/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID invalido")
                    return@get
                }

                val pavilhao = PavilhaoService.lerPorId(id)

                if (pavilhao != null) {
                    call.respond(HttpStatusCode.OK, pavilhao)
                } else {
                    call.respond(HttpStatusCode.NotFound, "pavilhao não encontrado")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao ler jogador")
            }
        }
    }
}