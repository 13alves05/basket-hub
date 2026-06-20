package com.est.Estatisticas

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import kotlinx.serialization.Serializable
import com.est.Jogador.JogadorService
import com.est.Equipa.EquipaService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

@Serializable
data class Estatisticas(
    val id: Int? = null,
    val jogadorId: Int,
    val equipaId: Int,
    val pontos: Int = 0,
    val assistencias: Int = 0,
    val rebounds: Int = 0,
    val estado: String
)

class EstatisticasService(val database: R2dbcDatabase) {

    object EstatisticasTabela : Table("estatisticas") {
        val id = integer("id").autoIncrement()
        val jogadorId = reference("jogador_id", JogadorService.Jogadores.id)
        val equipaId = reference("equipa_id", EquipaService.Equipas.id)
        val pontos = integer("pontos")
        val assistencias = integer("assistencias")
        val rebounds = integer("rebounds")
        val estado = varchar("estado", 20)

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun createSchema() {
        suspendTransaction(database) {
            SchemaUtils.create(EstatisticasTabela)
        }
    }

    suspend fun registarMovimento(dados: Estatisticas) {
        suspendTransaction(database) {
            EstatisticasTabela.update({
                (EstatisticasTabela.jogadorId eq dados.jogadorId) and
                        (EstatisticasTabela.estado eq "Ativo")
            }) {
                it[estado] = "Desativado"
            }

            EstatisticasTabela.insert {
                it[jogadorId] = dados.jogadorId
                it[equipaId] = dados.equipaId
                it[pontos] = dados.pontos
                it[assistencias] = dados.assistencias
                it[rebounds] = dados.rebounds
                it[estado] = dados.estado
            }
        }
    }

    suspend fun lerHistoricoJogador(jogadorId: Int): List<Estatisticas> {
        return suspendTransaction(database) {
            EstatisticasTabela.selectAll()
                .where { EstatisticasTabela.jogadorId eq jogadorId }
                .map { row ->
                    Estatisticas(
                        id = row[EstatisticasTabela.id],
                        jogadorId = row[EstatisticasTabela.jogadorId],
                        equipaId = row[EstatisticasTabela.equipaId],
                        pontos = row[EstatisticasTabela.pontos],
                        assistencias = row[EstatisticasTabela.assistencias],
                        rebounds = row[EstatisticasTabela.rebounds],
                        estado = row[EstatisticasTabela.estado]
                    )
                }.toList()
        }
    }

    suspend fun lerTodasAsEstatisticas(): List<Estatisticas> = suspendTransaction(database) {
        EstatisticasTabela.selectAll().map { row ->
            Estatisticas(
                id = row[EstatisticasTabela.id],
                jogadorId = row[EstatisticasTabela.jogadorId],
                equipaId = row[EstatisticasTabela.equipaId],
                pontos = row[EstatisticasTabela.pontos],
                assistencias = row[EstatisticasTabela.assistencias],
                rebounds = row[EstatisticasTabela.rebounds],
                estado = row[EstatisticasTabela.estado]
            )
        }.toList()
    }
}