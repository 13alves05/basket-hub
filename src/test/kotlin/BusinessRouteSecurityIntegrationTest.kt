package com.est.Security

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class BusinessRouteSecurityIntegrationTest {
    private data class TestCredentials(
        val database: R2dbcDatabase,
        val administratorToken: String,
        val visitorToken: String
    )

    private fun createCredentials(): TestCredentials = runBlocking {
        val databaseName = "business_route_security_" + UUID.randomUUID().toString().replace("-", "")
        val database = R2dbcDatabase.connect(
            url = "r2dbc:h2:file:///./build/$databaseName",
            user = "sa",
            password = ""
        )
        SecuritySchema.create(database)

        val bootstrapToken = "local-development-bootstrap-token-32-bytes"
        val bootstrap = AdminBootstrapService(database, bootstrapToken)
        assertEquals(
            BootstrapResult.CREATED,
            bootstrap.createFirstAdministrator(
                providedToken = bootstrapToken,
                email = "admin@example.com",
                password = "BasketHub-Seguro-2026"
            )
        )

        val sessions = SessionService(database)
        val administratorLogin = sessions.login("admin@example.com", "BasketHub-Seguro-2026")
        val administratorSession = administratorLogin.second ?: error("Administrador sem sessão.")
        val administratorId = administratorSession.account.accountId
        val administratorToken = administratorSession.token

        val management = RoleManagementService(database)
        val (createResult, visitorId) = management.createAccount(
            administratorId,
            "visitor@example.com",
            "Visitor-Password-2026"
        )
        assertEquals(RoleManagementResult.SUCCESS, createResult)
        val accountId = visitorId!!
        assertEquals(
            RoleManagementResult.SUCCESS,
            management.assignRole(
                administratorId,
                accountId,
                "VISITANTE"
            )
        )
        val visitorLogin = sessions.login("visitor@example.com", "Visitor-Password-2026")
        val visitorToken = visitorLogin.second?.token ?: error("Visitante sem sessão.")

        TestCredentials(database, administratorToken, visitorToken)
    }

    @Test
    fun privateRoutesRequireAdministratorAndExplicitPublicReadsRemainAvailable() {
        val credentials = createCredentials()

        testApplication {
            application {
                routing {
                    enforceTemporaryAdministratorPolicy(SessionService(credentials.database))
                    get("/private-resource") { call.respondText("private") }
                    get("/eventos/1") { call.respondText("public") }
                }
            }

            assertEquals(
                HttpStatusCode.Unauthorized,
                client.get("/private-resource").status
            )

            assertEquals(
                HttpStatusCode.Forbidden,
                client.get("/private-resource") {
                    header(HttpHeaders.Authorization, "Bearer ${credentials.visitorToken}")
                }.status
            )

            assertEquals(
                HttpStatusCode.OK,
                client.get("/private-resource") {
                    header(HttpHeaders.Authorization, "Bearer ${credentials.administratorToken}")
                }.status
            )

            assertEquals(
                HttpStatusCode.OK,
                client.get("/eventos/1").status
            )
        }
    }
}
