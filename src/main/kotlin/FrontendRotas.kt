package com.est

import com.est.DBUtils.DatabaseCreation
import com.est.Evento.*
import com.est.Inscricao.Inscricao
import com.est.Patrocinador.Patrocinador
import com.est.Organizador.Organizador
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.ThymeleafContent

fun Application.FrontendRotas() {
    routing {

        // ==========================================
        // 1. MENU PRINCIPAL
        // ==========================================
        get("/hub") {
            call.respond(ThymeleafContent("index", mapOf("user" to com.est.ThymeleafUser(1, "JeBron Lames"))))
        }

        // ==========================================
        // 2. PERFIL: ORGANIZADOR
        // ==========================================
        get("/organizador") {
            val accept = call.request.headers[HttpHeaders.Accept]
            if (accept?.contains("application/json") == true) {
                val lista: List<Organizador> = DatabaseCreation.OrganizadorService.lerTodos()
                call.respond(lista)
            } else {
                call.respond(ThymeleafContent("organizador", mapOf(
                    "pavilhoes" to DatabaseCreation.PavilhaoService.lerTodos(),
                    "organizadores" to DatabaseCreation.OrganizadorService.lerTodos(),
                    "eventos" to DatabaseCreation.EventoService.lerEventos()
                )))
            }
        }

        // ==========================================
        // 3. PERFIL: GESTOR DE EQUIPA
        // ==========================================
        get("/gestor-equipa") {
            call.respond(ThymeleafContent("gestor_equipa", mapOf(
                "equipas" to DatabaseCreation.EquipaService.lerEquipas(),
                "eventos" to DatabaseCreation.EventoService.lerEventos()
            )))
        }

        // ==========================================
        // 4. PERFIL: JOGADOR
        // ==========================================
        get("/jogador") {
            val accept = call.request.headers[HttpHeaders.Accept]
            if (accept?.contains("application/json") == true) {
                call.respond(DatabaseCreation.JogadorService.lerJogadores())
            } else {
                call.respond(ThymeleafContent("jogador", mapOf(
                    "jogadores" to DatabaseCreation.JogadorService.lerJogadores(),
                    "estatisticas" to DatabaseCreation.EstatisticasService.lerTodasAsEstatisticas()
                )))
            }
        }

        // ==========================================
        // 5. PERFIL: PATROCINADOR
        // ==========================================
        get("/patrocinadores") {
            val accept = call.request.headers[HttpHeaders.Accept]
            if (accept?.contains("application/json") == true) {
                val lista: List<Patrocinador> = DatabaseCreation.PatrocinadorService.lerPatrocinadores()
                call.respond(lista)
            } else {
                call.respond(ThymeleafContent("patrocinadores", mapOf(
                    "eventos" to DatabaseCreation.EventoService.lerEventos()
                )))
            }
        }

        post("/patrocinadores/financiar") {
            val params = call.receiveParameters()
            val contrato = Patrocinador(
                null,
                params["nome"] ?: "",
                params["empresa"] ?: "",
                params["valorContrato"]?.toDoubleOrNull() ?: 0.0,
                params["eventoId"]?.toIntOrNull()
            )
            DatabaseCreation.PatrocinadorService.criarPatrocinador(contrato)
            call.respondRedirect("/patrocinadores")
        }

        // ==========================================
        // 6. Classificação e Agendamento
        // ==========================================

        get("/classificacoes/geral") {
            val todosJogos = DatabaseCreation.JogoService.lerJogos()
            val classificacao = DatabaseCreation.ClassificacaoService.calcularClassificacao(todosJogos)
            call.respond(classificacao)
        }

        post("/eventos/{id}/agendamento") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                val resultado = DatabaseCreation.AgendamentoService.gerarOuAvancarChaveamento(id)
                call.respond(HttpStatusCode.OK, resultado)
            } else {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
            }
        }

        // ==========================================
        // 7. PERFIL: PÚBLICO
        // ==========================================
        get("/publico") {
            call.respond(ThymeleafContent("publico", mapOf("eventos" to DatabaseCreation.EventoService.lerEventos())))
        }
    }
}