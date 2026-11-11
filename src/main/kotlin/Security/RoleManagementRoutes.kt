package com.est.Security

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

@Serializable
data class CreateAccountRequest(
    val email: String,
    val initialPassword: String
)

@Serializable
data class AssignRoleRequest(
    val roleCode: String
)

@Serializable
data class AssignScopeRequest(
    val roleCode: String,
    val scopeType: String,
    val scopeId: String
)

fun Application.configureRoleManagementRoutes(database: R2dbcDatabase) {
    val sessions = SessionService(database)
    val management = RoleManagementService(database)

    routing {
        post("/admin/accounts") {
            val administrator = requireAdministrator(
                sessions,
                call.request.headers["Authorization"]
            )
            if (administrator == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acesso reservado ao Administrador."))
                return@post
            }

            val request = try {
                call.receive<CreateAccountRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Pedido inválido."))
                return@post
            }

            val (result, accountId) = try {
                management.createAccount(administrator.accountId, request.email, request.initialPassword)
            } catch (_: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Não foi possível criar a conta."))
                return@post
            }

            when (result) {
                RoleManagementResult.SUCCESS ->
                    call.respond(HttpStatusCode.Created, mapOf("accountId" to accountId))
                RoleManagementResult.FORBIDDEN ->
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acesso reservado ao Administrador."))
                RoleManagementResult.INVALID_INPUT ->
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Email ou palavra-passe inicial inválidos."))
                RoleManagementResult.EMAIL_ALREADY_EXISTS ->
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Já existe uma conta com esse email."))
                else ->
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Não foi possível criar a conta."))
            }
        }

        post("/admin/accounts/{accountId}/roles") {
            val administrator = requireAdministrator(sessions, call.request.headers["Authorization"])
            if (administrator == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acesso reservado ao Administrador."))
                return@post
            }
            val accountId = call.parameters["accountId"]
            if (accountId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Conta inválida."))
                return@post
            }
            val request = try {
                call.receive<AssignRoleRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Pedido inválido."))
                return@post
            }

            respondRoleManagement(
                call,
                management.assignRole(administrator.accountId, accountId, request.roleCode)
            )
        }

        delete("/admin/accounts/{accountId}/roles/{roleCode}") {
            val administrator = requireAdministrator(sessions, call.request.headers["Authorization"])
            if (administrator == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acesso reservado ao Administrador."))
                return@delete
            }
            val accountId = call.parameters["accountId"]
            val roleCode = call.parameters["roleCode"]
            if (accountId.isNullOrBlank() || roleCode.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Conta ou papel inválido."))
                return@delete
            }

            respondRoleManagement(
                call,
                management.removeRole(administrator.accountId, accountId, roleCode)
            )
        }

        post("/admin/accounts/{accountId}/scopes") {
            val administrator = requireAdministrator(sessions, call.request.headers["Authorization"])
            if (administrator == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acesso reservado ao Administrador."))
                return@post
            }
            val accountId = call.parameters["accountId"]
            if (accountId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Conta inválida."))
                return@post
            }
            val request = try {
                call.receive<AssignScopeRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Pedido inválido."))
                return@post
            }

            respondRoleManagement(
                call,
                management.assignScope(
                    administrator.accountId,
                    accountId,
                    request.roleCode,
                    request.scopeType,
                    request.scopeId
                )
            )
        }

        delete("/admin/accounts/{accountId}/scopes") {
            val administrator = requireAdministrator(sessions, call.request.headers["Authorization"])
            if (administrator == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acesso reservado ao Administrador."))
                return@delete
            }
            val accountId = call.parameters["accountId"]
            if (accountId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Conta inválida."))
                return@delete
            }
            val request = try {
                call.receive<AssignScopeRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Pedido inválido."))
                return@delete
            }

            respondRoleManagement(
                call,
                management.revokeScope(
                    administrator.accountId,
                    accountId,
                    request.roleCode,
                    request.scopeType,
                    request.scopeId
                )
            )
        }
    }
}

private suspend fun requireAdministrator(
    sessions: SessionService,
    authorizationHeader: String?
): AuthenticatedAccount? {
    val token = authorizationHeader
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substringAfter(' ')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    val account = sessions.authenticate(token) ?: return null
    return account.takeIf { "ADMINISTRADOR" in it.roles }
}

private suspend fun respondRoleManagement(
    call: io.ktor.server.application.ApplicationCall,
    result: RoleManagementResult
) {
    when (result) {
        RoleManagementResult.SUCCESS ->
            call.respond(HttpStatusCode.OK, mapOf("status" to "Operação concluída."))
        RoleManagementResult.FORBIDDEN ->
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acesso reservado ao Administrador."))
        RoleManagementResult.NOT_FOUND ->
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Conta não encontrada."))
        RoleManagementResult.INVALID_ROLE, RoleManagementResult.INVALID_SCOPE, RoleManagementResult.INVALID_INPUT ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Papel ou âmbito inválido."))
        RoleManagementResult.ROLE_REQUIRED ->
            call.respond(HttpStatusCode.Conflict, mapOf("error" to "A conta ainda não possui esse papel."))
        RoleManagementResult.ALREADY_ASSIGNED ->
            call.respond(HttpStatusCode.Conflict, mapOf("error" to "Atribuição já existente."))
        RoleManagementResult.NOT_ASSIGNED ->
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Atribuição não encontrada."))
        RoleManagementResult.LAST_ADMIN_PROTECTED ->
            call.respond(HttpStatusCode.Conflict, mapOf("error" to "Não é possível remover o último Administrador ativo."))
        RoleManagementResult.EMAIL_ALREADY_EXISTS ->
            call.respond(HttpStatusCode.Conflict, mapOf("error" to "Já existe uma conta com esse email."))
    }
}
