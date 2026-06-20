package com.est.Patrocinador

import com.est.DBUtils.DBUtils
import com.est.DBUtils.DatabaseCreation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.Patrocinadores_Rotas() {
    val service = PatrocinadorService(DBUtils.database)

    routing {
        route("/patrocinadores") {
            post {
                val dados = call.receive<Patrocinador>()
                service.criarPatrocinador(dados)
                call.respond(HttpStatusCode.Created)
            }
            get {
                val lista = service.lerPatrocinadores()
                call.respond(HttpStatusCode.OK, lista)
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id != null) {
                    val patrocinador = service.lerPatrocinadorPorId(id)
                    if (patrocinador != null) {
                        call.respond(HttpStatusCode.OK, patrocinador)
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
                    val dados = call.receive<Patrocinador>()
                    service.atualizarPatrocinador(id, dados)
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id != null) {
                    service.removerPatrocinador(id)
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}