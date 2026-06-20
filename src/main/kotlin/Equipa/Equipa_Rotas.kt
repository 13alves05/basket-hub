package com.est.Equipa

import com.est.DBUtils.DBUtils
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.Equipas_Rotas() {
    val equipaService = EquipaService(DBUtils.database)

    routing {
        post("/equipas") {
            try {
                val equipa = call.receive<Equipa>()
                equipaService.criarEquipa(equipa)
                call.respond(HttpStatusCode.Created, "Equipa criada")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Erro")
            }
        }

        get("/equipas") {
            try {
                val lista = equipaService.lerEquipas()
                call.respond(HttpStatusCode.OK, lista)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }

        put("/equipas/{id}") {
            val id = call.parameters["id"]!!.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
            val equipa = call.receive<Equipa>()
            equipaService.atualizarEquipa(id, equipa)
            call.respond(HttpStatusCode.OK, "Equipa updated")
        }

        delete("/equipas/{id}") {
            val id = call.parameters["id"]!!.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            equipaService.removerEquipa(id)
            call.respond(HttpStatusCode.OK, "Equipa removida")
        }

        get("/equipas/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID invalido")
                    return@get
                }
                val equipa = equipaService.lerEquipaPorId(id)
                if (equipa != null) {
                    call.respond(HttpStatusCode.OK, equipa)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Equipa não encontrada")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }
    }
}