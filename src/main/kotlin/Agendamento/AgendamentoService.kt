package com.est.Agendamento

import com.est.Evento.EventoService
import com.est.Evento.Torneio
import com.est.Inscricao.InscricaoService
import com.est.Jogo.Jogo
import com.est.Jogo.JogoService
import kotlinx.serialization.Serializable

@Serializable
data class ResultadoAgendamento(
    val eventoId: Int,
    val rondaGerada: Int,
    val jogosCriados: Int,
    val equipasComBye: List<Int> = emptyList(),
    val campeaoId: Int? = null,
    val mensagem: String
)

class AgendamentoService(
    private val eventoService: EventoService,
    private val inscricaoService: InscricaoService,
    private val jogoService: JogoService
) {

    suspend fun gerarOuAvancarChaveamento(eventoId: Int): ResultadoAgendamento {
        val evento = eventoService.lerEventoPorId(eventoId)
            ?: throw IllegalArgumentException("Evento não encontrado")

        if (evento !is Torneio) {
            throw IllegalStateException("Agendamento apenas para Torneios")
        }

        val jogosExistentes = jogoService.lerJogosPorEvento(eventoId)

        return if (jogosExistentes.isEmpty()) {
            gerarPrimeiraRonda(eventoId, evento.pavilhaoId)
        } else {
            avancarRonda(eventoId, evento.pavilhaoId, jogosExistentes)
        }
    }

    private suspend fun gerarPrimeiraRonda(eventoId: Int, pavilhaoId: Int): ResultadoAgendamento {
        val inscricoes = inscricaoService.lerInscricoes().filter { it.eventoId == eventoId }
        val ordemJogo = inscricoes.map { it.equipaId }.toMutableList()
        val ronda = 1
        val equipasComBye = mutableListOf<Int>()
        var jogosCriados = 0

        if (ordemJogo.isEmpty()) {
            throw IllegalStateException("Nenhuma equipa inscrita neste torneio.")
        }

        if (ordemJogo.size % 2 != 0) {
            val equipaComBye = ordemJogo.removeAt(ordemJogo.size - 1)
            equipasComBye.add(equipaComBye)
            jogoService.criarJogo(
                Jogo(
                    eventoId = eventoId,
                    ronda = ronda,
                    equipaCasaId = equipaComBye,
                    equipaForaId = null,
                    pavilhaoId = pavilhaoId,
                    pontosCasa = 0,
                    pontosFora = 0,
                    dataHora = "Ronda $ronda (bye)"
                )
            )
            jogosCriados++
        }

        var indice = 0
        while (indice < ordemJogo.size) {
            jogoService.criarJogo(
                Jogo(
                    eventoId = eventoId,
                    ronda = ronda,
                    equipaCasaId = ordemJogo[indice],
                    equipaForaId = ordemJogo[indice + 1],
                    pavilhaoId = pavilhaoId,
                    pontosCasa = 0,
                    pontosFora = 0,
                    dataHora = "Ronda $ronda - Jogo ${(indice / 2) + 1}"
                )
            )
            jogosCriados++
            indice += 2
        }

        return ResultadoAgendamento(
            eventoId = eventoId,
            rondaGerada = ronda,
            jogosCriados = jogosCriados,
            equipasComBye = equipasComBye,
            mensagem = "Ronda $ronda gerada com sucesso!"
        )
    }

    private suspend fun avancarRonda(eventoId: Int, pavilhaoId: Int, jogosExistentes: List<Jogo>): ResultadoAgendamento {
        val ultimaRonda = jogosExistentes.maxOf { it.ronda }
        val jogosUltimaRonda = jogosExistentes.filter { it.ronda == ultimaRonda }

        // Verifica se todos os jogos reais da ronda anterior foram decididos
        val naoDecididos = jogosUltimaRonda.filter { it.equipaForaId != null && it.pontosCasa == it.pontosFora }
        if (naoDecididos.isNotEmpty()) {
            throw IllegalStateException("A ronda $ultimaRonda ainda não terminou. Existem jogos sem resultado decidido.")
        }

        val vencedores = jogosUltimaRonda.mapNotNull { it.vencedorId() }.toMutableList()

        if (vencedores.size == 1) {
            return ResultadoAgendamento(
                eventoId = eventoId,
                rondaGerada = ultimaRonda,
                jogosCriados = 0,
                campeaoId = vencedores.first(),
                mensagem = "O Torneio terminou! O grande campeão é a Equipa ${vencedores.first()}."
            )
        }

        val ronda = ultimaRonda + 1
        val ordemJogo = vencedores
        val equipasComBye = mutableListOf<Int>()
        var jogosCriados = 0

        if (ordemJogo.size % 2 != 0) {
            val equipaComBye = ordemJogo.removeAt(ordemJogo.size - 1)
            equipasComBye.add(equipaComBye)
            jogoService.criarJogo(
                Jogo(
                    eventoId = eventoId,
                    ronda = ronda,
                    equipaCasaId = equipaComBye,
                    equipaForaId = null,
                    pavilhaoId = pavilhaoId,
                    pontosCasa = 0,
                    pontosFora = 0,
                    dataHora = "Ronda $ronda (bye)"
                )
            )
            jogosCriados++
        }

        var indice = 0
        while (indice < ordemJogo.size) {
            jogoService.criarJogo(
                Jogo(
                    eventoId = eventoId,
                    ronda = ronda,
                    equipaCasaId = ordemJogo[indice],
                    equipaForaId = ordemJogo[indice + 1],
                    pavilhaoId = pavilhaoId,
                    pontosCasa = 0,
                    pontosFora = 0,
                    dataHora = "Ronda $ronda - Jogo ${(indice / 2) + 1}"
                )
            )
            jogosCriados++
            indice += 2
        }

        return ResultadoAgendamento(
            eventoId = eventoId,
            rondaGerada = ronda,
            jogosCriados = jogosCriados,
            equipasComBye = equipasComBye,
            mensagem = "Ronda $ronda avançada e gerada com sucesso!"
        )
    }
}