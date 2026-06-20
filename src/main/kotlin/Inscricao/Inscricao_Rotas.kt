package com.est.Inscricao

import com.est.DBUtils.DBUtils
import io.ktor.server.application.Application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.route

import com.est.Inscricao.*

fun Application.Inscricao_Rotas() {
    val service = InscricaoService(DBUtils.database)

    routing {
            route("/inscricoes") {
                post {
                    val dados = call.receive<Inscricao>()
                    service.criarInscricao(dados)
                    call.respond(HttpStatusCode.Created)
                }
                get {
                    val lista = service.lerInscricoes()
                    call.respond(HttpStatusCode.OK, lista)
                }
                get("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id != null) {
                        val inscricao = service.lerInscricaoPorId(id)
                        if (inscricao != null) {
                            call.respond(HttpStatusCode.OK, inscricao)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                put("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id != null) {
                        val dados = call.receive<Inscricao>()
                        service.atualizarInscricao(id, dados)
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                delete("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id != null) {
                        service.removerInscricao(id)
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }