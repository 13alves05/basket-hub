package com.est

import com.est.Jogo.Jogos_Rotas
import com.est.DBUtils.DatabaseCreation
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
 * Esta versão ainda não tem autenticação/autorização nas rotas de gestão.
 * Falha de forma segura se alguém tentar iniciá-la como serviço de produção.
 */
internal fun ensureDeploymentEnvironmentIsSupported(environment: String?) {
    if (environment?.trim()?.equals("production", ignoreCase = true) == true) {
        throw IllegalStateException(
            "Basket Hub não pode arrancar em produção: autenticação e autorização ainda não estão implementadas."
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
