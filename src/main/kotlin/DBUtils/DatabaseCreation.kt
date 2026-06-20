package com.est.DBUtils

import com.est.Agendamento.AgendamentoService
import com.est.Classificacao.ClassificacaoService
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import com.est.Jogador.JogadorService
import com.est.Organizador.OrganizadorService
import com.est.Pavilhao.PavilhaoService
import com.est.Equipa.EquipaService
import com.est.Evento.EventoService
import com.est.Patrocinador.PatrocinadorService
import com.est.Inscricao.InscricaoService
import com.est.Jogo.JogoService
import com.est.Estatisticas.EstatisticasService

object DatabaseCreation {
    private lateinit var database: R2dbcDatabase

    lateinit var PavilhaoService: PavilhaoService
        private set
    lateinit var OrganizadorService: OrganizadorService
        private set
    lateinit var EquipaService: EquipaService
        private set
    lateinit var JogadorService: JogadorService
        private set
    lateinit var EventoService: EventoService
        private set
    lateinit var PatrocinadorService: PatrocinadorService
        private set
    lateinit var InscricaoService: InscricaoService
        private set
    lateinit var JogoService: JogoService
        private set
    lateinit var EstatisticasService: EstatisticasService
        private set
    lateinit var ClassificacaoService: ClassificacaoService
        private set
    lateinit var AgendamentoService: AgendamentoService
        private set

    suspend fun init() {
        database = DBUtils.database

        // 1. Tabelas Independentes (Base)
        PavilhaoService = PavilhaoService(database)
        PavilhaoService.createSchema()

        OrganizadorService = OrganizadorService(database)
        OrganizadorService.createSchema()

        EquipaService = EquipaService(database)
        EquipaService.createSchema()

        JogadorService = JogadorService(database)
        JogadorService.createSchema()

        // 2. Tabelas Dependentes
        EventoService = EventoService(database)
        EventoService.createSchema()

        PatrocinadorService = PatrocinadorService(database)
        PatrocinadorService.createSchema()

        InscricaoService = InscricaoService(database)
        InscricaoService.createSchema()

        JogoService = JogoService(database)
        JogoService.createSchema()

        // 3. Tabelas de Associação
        EstatisticasService = EstatisticasService(database)
        EstatisticasService.createSchema()

        // 4. Inicialização dos serviços de Lógica de Negócio
        // Dependem de instâncias criadas anteriormente
        ClassificacaoService = ClassificacaoService(JogoService)

        AgendamentoService = AgendamentoService(
            EventoService,
            InscricaoService,
            JogoService
        )
    }
}