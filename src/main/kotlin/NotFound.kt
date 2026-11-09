package com.est

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.ThymeleafContent

/**
 * Handles requests that do not match any registered application route.
 *
 * This is registered after the feature routes so valid API and frontend
 * endpoints keep their existing handlers.
 */
fun Application.configureNotFound() {
    routing {
        route("/{...}") {
            handle {
                val accept = call.request.headers[HttpHeaders.Accept].orEmpty()

                if (accept.contains(ContentType.Application.Json.toString(), ignoreCase = true)) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf(
                            "error" to "Not Found",
                            "path" to call.request.uri
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ThymeleafContent(
                            "404",
                            mapOf("path" to call.request.uri)
                        )
                    )
                }
            }
        }
    }
}
