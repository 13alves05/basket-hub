package com.est

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureHttp() {
    val allowedHosts = System.getenv("CORS_ALLOWED_HOSTS")
        ?.split(",")
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        .orEmpty()

    install(CORS) {
        // Same-origin requests do not require CORS. Cross-origin access is denied
        // unless an explicit host allowlist is configured for the deployment.
        allowedHosts.forEach { host ->
            allowHost(host, schemes = listOf("http", "https"))
        }

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("MyCustomHeader")
    }
}
