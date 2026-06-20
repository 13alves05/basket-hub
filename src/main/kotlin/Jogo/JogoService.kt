package com.est.Jogo

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import com.est.Evento.EventoService
import com.est.Equipa.EquipaService
import com.est.Pavilhao.PavilhaoService
import com.est.Repository.Repository

@Serializable
data class Jogo(
    val id: Int? = null,
    val eventoId: Int,
    val ronda: Int = 1,
    val equipaCasaId: Int,
    val equipaForaId: Int? = null, // null = "bye": a equipaCasaId passa automaticamente à ronda seguinte
    val pavilhaoId: Int,
    val pontosCasa: Int = 0,
    val pontosFora: Int = 0,
    val dataHora: String
) {
    /** ID da equipa vencedora deste jogo, ou null se ainda não houver resultado decidido. */
    fun vencedorId(): Int? = when {
        equipaForaId == null -> equipaCasaId
        pontosCasa > pontosFora -> equipaCasaId
        pontosFora > pontosCasa -> equipaForaId
        else -> null
    }
}

class JogoService(val database: R2dbcDatabase) : Repository<Jogo, Int> {

    object Jogos : Table("jogos") {
        val id = integer("id").autoIncrement()

        // Multiplas Foreign Keys mapeadas corretamente
        val eventoId = reference("evento_id", EventoService.Eventos.id)
        val ronda = integer("ronda").default(1)
        val equipaCasaId = reference("equipa_casa_id", EquipaService.Equipas.id)
        val equipaForaId = reference("equipa_fora_id", EquipaService.Equipas.id).nullable()
        val pavilhaoId = reference("pavilhao_id", PavilhaoService.Pavilhoes.id)

        val pontosCasa = integer("pontos_casa")
        val pontosFora = integer("pontos_fora")
        val dataHora = varchar("data_hora", 50)

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun createSchema() {
        suspendTransaction(database) {
            SchemaUtils.create(Jogos)
        }
    }

    override suspend fun criar(item: Jogo) {
        suspendTransaction(database) {
            Jogos.insert {
                it[eventoId] = item.eventoId
                it[ronda] = item.ronda
                it[equipaCasaId] = item.equipaCasaId
                it[equipaForaId] = item.equipaForaId
                it[pavilhaoId] = item.pavilhaoId
                it[pontosCasa] = item.pontosCasa
                it[pontosFora] = item.pontosFora
                it[dataHora] = item.dataHora
            }
        }
    }

    override suspend fun lerTodos(): List<Jogo> {
        return suspendTransaction(database) {
            Jogos.selectAll().map {
                Jogo(
                    id = it[Jogos.id],
                    eventoId = it[Jogos.eventoId],
                    ronda = it[Jogos.ronda],
                    equipaCasaId = it[Jogos.equipaCasaId],
                    equipaForaId = it[Jogos.equipaForaId],
                    pavilhaoId = it[Jogos.pavilhaoId],
                    pontosCasa = it[Jogos.pontosCasa],
                    pontosFora = it[Jogos.pontosFora],
                    dataHora = it[Jogos.dataHora]
                )
            }.toList()
        }
    }

    override suspend fun lerPorId(id: Int): Jogo? {
        return suspendTransaction(database) {
            Jogos.selectAll()
                .where { Jogos.id eq id }
                .map {
                    Jogo(
                        id = it[Jogos.id],
                        eventoId = it[Jogos.eventoId],
                        ronda = it[Jogos.ronda],
                        equipaCasaId = it[Jogos.equipaCasaId],
                        equipaForaId = it[Jogos.equipaForaId],
                        pavilhaoId = it[Jogos.pavilhaoId],
                        pontosCasa = it[Jogos.pontosCasa],
                        pontosFora = it[Jogos.pontosFora],
                        dataHora = it[Jogos.dataHora]
                    )
                }.singleOrNull()
        }
    }

    override suspend fun atualizar(id: Int, item: Jogo) {
        suspendTransaction(database) {
            Jogos.update({ Jogos.id eq id }) {
                it[eventoId] = item.eventoId
                it[ronda] = item.ronda
                it[equipaCasaId] = item.equipaCasaId
                it[equipaForaId] = item.equipaForaId
                it[pavilhaoId] = item.pavilhaoId
                it[pontosCasa] = item.pontosCasa
                it[pontosFora] = item.pontosFora
                it[dataHora] = item.dataHora
            }
        }
    }

    override suspend fun remover(id: Int) {
        suspendTransaction(database) {
            Jogos.deleteWhere { Jogos.id eq id }
        }
    }

    // Aliases com os nomes que o Jogos_Rotas.kt já existente utiliza,
    // para não ser preciso voltar a tocar nesse ficheiro.
    suspend fun criarJogo(dados: Jogo) = criar(dados)
    suspend fun lerJogos(): List<Jogo> = lerTodos()
    suspend fun lerJogoPorId(id: Int): Jogo? = lerPorId(id)
    suspend fun atualizarJogo(id: Int, dados: Jogo) = atualizar(id, dados)
    suspend fun removerJogo(id: Int) = remover(id)

    suspend fun lerJogosPorEvento(eventoId: Int): List<Jogo> {
        return suspendTransaction(database) {
            Jogos.selectAll()
                .where { Jogos.eventoId eq eventoId }
                .map {
                    Jogo(
                        id = it[Jogos.id],
                        eventoId = it[Jogos.eventoId],
                        ronda = it[Jogos.ronda],
                        equipaCasaId = it[Jogos.equipaCasaId],
                        equipaForaId = it[Jogos.equipaForaId],
                        pavilhaoId = it[Jogos.pavilhaoId],
                        pontosCasa = it[Jogos.pontosCasa],
                        pontosFora = it[Jogos.pontosFora],
                        dataHora = it[Jogos.dataHora]
                    )
                }.toList()
        }
    }

    suspend fun lerJogosPorEventoERonda(eventoId: Int, ronda: Int): List<Jogo> {
        return suspendTransaction(database) {
            Jogos.selectAll()
                .where { (Jogos.eventoId eq eventoId) and (Jogos.ronda eq ronda) }
                .map {
                    Jogo(
                        id = it[Jogos.id],
                        eventoId = it[Jogos.eventoId],
                        ronda = it[Jogos.ronda],
                        equipaCasaId = it[Jogos.equipaCasaId],
                        equipaForaId = it[Jogos.equipaForaId],
                        pavilhaoId = it[Jogos.pavilhaoId],
                        pontosCasa = it[Jogos.pontosCasa],
                        pontosFora = it[Jogos.pontosFora],
                        dataHora = it[Jogos.dataHora]
                    )
                }.toList()
        }
    }
}