package com.est.Organizador

import com.est.DBUtils.DBUtils
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.Organizadores_Rotas() {
    val service = OrganizadorService(DBUtils.database)

    routing {
            post("/organizador") {
                try {
                    val organizador = call.receive<Organizador>()
                    service.criarOrganizador(organizador)
                    call.respond(HttpStatusCode.Created, "Organizador criado com sucesso")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Erro ao criar Organizador")
                }
            }

            get("/organizadores") {
                try {
                    call.respond(HttpStatusCode.OK, service.lerTodos())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao ler organizadores")
                }
            }

            get("organizador/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, "ID inválido")
                        return@get
                    }
                    val organizador = service.lerPorId(id)
                    if (organizador != null) {
                        call.respond(HttpStatusCode.OK, organizador)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Organizador não encontrado")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao buscar organizador")
                }
            }

            put("organizador/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, "ID inválido")
                try {
                    val organizador = call.receive<Organizador>()
                    service.atualizar(id, organizador)
                    call.respond(HttpStatusCode.OK, "Organizador atualizado")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao atualizar organizador")
                }
            }

            delete("organizador/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest, "ID inválido")
                try {
                    service.remover(id)
                    call.respond(HttpStatusCode.OK, "Organizador removido")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao remover organizador")
                }
            }
    }
}