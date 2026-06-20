package com.est.Evento

import com.est.DBUtils.DBUtils
import com.est.DBUtils.DatabaseCreation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.Eventos_Rotas() {
    val service = EventoService(DBUtils.database)

    routing {
        route("/eventos") {

            // Criar evento (Torneio, Amigavel ou ClinicaDeTreino - ver campo "type" no JSON)
            post {
                try {
                    val dados = call.receive<Evento>()
                    service.criarEvento(dados)
                    call.respond(HttpStatusCode.Created, "Evento criado com sucesso")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Erro ao criar evento")
                }
            }

            // Buscar todos os eventos
            get {
                try {
                    val lista = service.lerEventos()
                    call.respond(HttpStatusCode.OK, lista)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao ler eventos")
                }
            }

            // Ler evento por ID
            get("/{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, "ID inválido")
                        return@get
                    }

                    val evento = service.lerEventoPorId(id)
                    if (evento != null) {
                        call.respond(HttpStatusCode.OK, evento)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Evento não encontrado")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao ler evento")
                }
            }

            // Atualizar evento
            put("/{id}") {
                try {
                    val id = call.parameters["id"]!!.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                    val dados = call.receive<Evento>()
                    service.atualizarEvento(id, dados)
                    call.respond(HttpStatusCode.OK, "Evento atualizado")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao atualizar evento")
                }
            }

            // Apagar evento
            delete("/{id}") {
                try {
                    val id = call.parameters["id"]!!.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    service.removerEvento(id)
                    call.respond(HttpStatusCode.OK, "Evento removido")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro ao remover evento")
                }
            }
        }
    }
}