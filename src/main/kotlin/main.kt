package com.est

import com.est.Jogo.Jogos_Rotas
import com.est.DBUtils.DatabaseCreation
import com.est.DBUtils.DBUtils
import com.est.Security.configureAuthRoutes
import com.est.Security.configureRoleManagementRoutes
import com.est.Jogador.Jogadores_Rotas
import com.est.Pavilhao.Pavilhoes_Rotas
import com.est.Organizador.Organizadores_Rotas
import io.ktor.server.application.*
import com.est.Equipa.Equipas_Rotas
import com.est.Estatisticas.Estatisticas_Rotas
import com.est.Evento.Eventos_Rotas
import com.est.Patrocinador.Patrocinadores_Rotas
import com.est.Agendamento.Agendamento_Rotas
import com.est.Classificacao.Classificacoes_Rotas
import com.est.Inscricao.Inscricao_Rotas
import kotlinx.coroutines.runBlocking

/**
 * A autorização por papel e âmbito já está aplicada a parte das rotas.
 * As restantes continuam sob uma política provisória ou aguardam definição
 * das relações de domínio. A aplicação não pode arrancar em produção até
 * a cobertura de segurança estar concluída e validada.
 */
internal fun ensureDeploymentEnvironmentIsSupported(environment: String?) {
    if (environment?.trim()?.equals("production", ignoreCase = true) == true) {
        throw IllegalStateException(
            "Basket Hub não pode arrancar em produção: a cobertura de autenticação e autorização ainda não está concluída."
        )
    }
}

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    ensureDeploymentEnvironmentIsSupported(System.getenv("APP_ENV"))

    // 1. Carregar Plugins Primeiro
    configureSerialization()
    configureHttp()
    configureThymeleaf()

    // 2. Inicializar a Base de Dados
    runBlocking {
        DatabaseCreation.init()
    }

    // 3. Registar as Rotas
    configureAuthRoutes(DBUtils.database)
    configureRoleManagementRoutes(DBUtils.database)
    configureRouting()
    FrontendRotas()

    Jogadores_Rotas()
    Pavilhoes_Rotas()
    Organizadores_Rotas()
    Equipas_Rotas()
    Eventos_Rotas()
    Patrocinadores_Rotas()
    Inscricao_Rotas()
    Jogos_Rotas()
    Estatisticas_Rotas()
    Agendamento_Rotas()
    Classificacoes_Rotas()
}
