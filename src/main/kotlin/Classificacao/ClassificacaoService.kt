package com.est.Classificacao

import com.est.Jogo.Jogo
import com.est.Jogo.JogoService
import kotlinx.serialization.Serializable

@Serializable
data class LinhaClassificacaoDTO(
    val equipaId: Int,
    val nomeEquipa: String,
    val jogos: Int,
    val vitorias: Int,
    val derrotas: Int,
    val pontosMarcados: Int,
    val pontosSofridos: Int,
    val saldo: Int,
    val pontos: Int
)

/**
 * Serviço responsável pelo cálculo da classificação (Geral e por Torneio).
 * Regra de Ouro: Basquetebol não permite empates (pontosCasa != pontosFora).
 */
class ClassificacaoService(private val jogoService: JogoService) {

    /**
     * Calcula a classificação com base numa lista de jogos.
     * Exclui "byes" (equipaForaId == null) e jogos sem resultado definido.
     */
    suspend fun calcularClassificacao(jogos: List<Jogo>): List<LinhaClassificacaoDTO> {
        val jogosValidos = jogos.filter {
            it.equipaForaId != null && it.pontosCasa != it.pontosFora
        }

        return jogosValidos
            .flatMap { listOf(it.equipaCasaId to it, it.equipaForaId!! to it) }
            .groupBy { it.first }
            .map { (equipaId, jogosDaEquipa) ->
                var vitorias = 0
                var derrotas = 0
                var pontosMarcados = 0
                var pontosSofridos = 0

                jogosDaEquipa.forEach { (id, jogo) ->
                    val (marcou, sofreu) = if (id == jogo.equipaCasaId) {
                        jogo.pontosCasa to jogo.pontosFora
                    } else {
                        jogo.pontosFora to jogo.pontosCasa
                    }

                    pontosMarcados += marcou
                    pontosSofridos += sofreu

                    if (marcou > sofreu) vitorias++ else derrotas++
                }

                LinhaClassificacaoDTO(
                    equipaId = equipaId,
                    nomeEquipa = "Equipa $equipaId",
                    jogos = jogosDaEquipa.size,
                    vitorias = vitorias,
                    derrotas = derrotas,
                    pontosMarcados = pontosMarcados,
                    pontosSofridos = pontosSofridos,
                    saldo = pontosMarcados - pontosSofridos,
                    pontos = vitorias * 2
                )
            }.sortedByDescending { it.pontos }
    }
}