package com.est.Security

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EventBracketGenerationRouteIntegrationTest {
    private data class Credentials(val database: R2dbcDatabase, val admin: String, val organizer: String)

    private fun credentials(): Credentials = runBlocking {
        val name = "bracket_route_" + UUID.randomUUID().toString().replace("-", "")
        val database = R2dbcDatabase.connect(url = "r2dbc:h2:file:///./build/$name", user = "sa", password = "")
        SecuritySchema.create(database)
        val bootstrapToken = "local-development-bootstrap-token-32-bytes"
        val bootstrap = AdminBootstrapService(database, bootstrapToken)
        assertEquals(BootstrapResult.CREATED, bootstrap.createFirstAdministrator(bootstrapToken, "admin-bracket@example.com", "BasketHub-Admin-2026"))
        val sessions = SessionService(database)
        val adminSession = sessions.login("admin-bracket@example.com", "BasketHub-Admin-2026").second ?: error("Admin session missing")
        val management = RoleManagementService(database)
        val (result, organizerId) = management.createAccount(adminSession.account.accountId, "organizer-bracket@example.com", "BasketHub-Organizer-2026")
        assertEquals(RoleManagementResult.SUCCESS, result)
        val target = organizerId ?: error("Organizer account missing")
        assertEquals(RoleManagementResult.SUCCESS, management.assignRole(adminSession.account.accountId, target, "ORGANIZADOR"))
        assertEquals(RoleManagementResult.SUCCESS, management.assignScope(adminSession.account.accountId, target, "ORGANIZADOR", "EVENTO", "21"))
        val organizerSession = sessions.login("organizer-bracket@example.com", "BasketHub-Organizer-2026").second ?: error("Organizer session missing")
        Credentials(database, adminSession.token, organizerSession.token)
    }

    @Test
    fun organizerCanGenerateOnlyWithinAssignedEventAndAdminIsGlobal() {
        val c = credentials()
        val generated = mutableListOf<Int>()
        testApplication {
            application {
                routing {
                    eventBracketGenerationRoute(SessionService(c.database), AuthorizationService(c.database)) { eventId ->
                        generated.add(eventId)
                        "generated-$eventId"
                    }
                }
            }
            assertEquals(HttpStatusCode.Unauthorized, client.post("/eventos/21/agendamento").status)
            assertEquals(HttpStatusCode.Forbidden, client.post("/eventos/22/agendamento") { header(HttpHeaders.Authorization, "Bearer " + c.organizer) }.status)
            val allowed = client.post("/eventos/21/agendamento") { header(HttpHeaders.Authorization, "Bearer " + c.organizer) }
            assertEquals(HttpStatusCode.OK, allowed.status)
            assertEquals("generated-21", allowed.bodyAsText())
            val adminAllowed = client.post("/eventos/22/agendamento") { header(HttpHeaders.Authorization, "Bearer " + c.admin) }
            assertEquals(HttpStatusCode.OK, adminAllowed.status)
            assertEquals("generated-22", adminAllowed.bodyAsText())
            assertEquals(listOf(21, 22), generated)
        }
    }

    @Test
    fun invalidIdAndDomainErrorsMapToExpectedStatuses() {
        val c = credentials()
        testApplication {
            application {
                routing {
                    eventBracketGenerationRoute(SessionService(c.database), AuthorizationService(c.database)) { eventId ->
                        when (eventId) {
                            21 -> throw IllegalArgumentException("Evento não encontrado")
                            22 -> throw IllegalStateException("Torneio ainda não pode avançar")
                            else -> "generated-$eventId"
                        }
                    }
                }
            }
            assertEquals(HttpStatusCode.BadRequest, client.post("/eventos/abc/agendamento") { header(HttpHeaders.Authorization, "Bearer " + c.admin) }.status)
            assertEquals(HttpStatusCode.NotFound, client.post("/eventos/21/agendamento") { header(HttpHeaders.Authorization, "Bearer " + c.admin) }.status)
            assertEquals(HttpStatusCode.Conflict, client.post("/eventos/22/agendamento") { header(HttpHeaders.Authorization, "Bearer " + c.admin) }.status)
        }
    }
}