package com.est

import com.est.Jogador.JogadorService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.ThymeleafContent
import io.ktor.server.http.content.*

fun Application.configureRouting() {
    routing {

        get("/") {
            call.respond(ThymeleafContent("index", mapOf("user" to ThymeleafUser(1, "user1"))))
        }

        get("/html-thymeleaf") {
            call.respond(ThymeleafContent("index", mapOf("user" to ThymeleafUser(1, "user1"))))
        }

        staticResources("/static", "static")

        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }

        get("/test") {
            call.respond("OK")
        }
    }
}