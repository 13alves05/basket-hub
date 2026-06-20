package com.est.Jogador

import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

@Serializable
class Jogador (
    val nome: String,
    val posicao: String,
    val n_camisola: Int,
    val id: Int? = null
)

class JogadorService(val database: R2dbcDatabase) {

    object Jogadores : Table("jogadores") {
        val id = integer("id").autoIncrement()
        val nome = varchar("nome", 100)
        val posicao = varchar("posicao", 50)
        val n_camisola = integer("n_camisola")

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun createSchema() {
        suspendTransaction(database) {
            SchemaUtils.create(Jogadores)
        }
    }

    suspend fun criarJogador(dados: Jogador) {
        suspendTransaction(database) {
            Jogadores.insert {
                it[nome] = dados.nome
                it[posicao] = dados.posicao
                it[n_camisola] = dados.n_camisola
            }
        }
    }

    suspend fun lerJogadores(): List<Jogador> {
        return suspendTransaction(database) {
            Jogadores.selectAll()
                .map {
                    Jogador(
                        nome = it[Jogadores.nome],
                        posicao = it[Jogadores.posicao],
                        n_camisola = it[Jogadores.n_camisola],
                        id = it[Jogadores.id]
                    )
                }
                .toList()
        }
    }

    suspend fun lerJogadorPorId(id: Int): Jogador? {
        return suspendTransaction(database) {
            Jogadores.selectAll()
                .where { Jogadores.id eq id }
                .map {
                    Jogador(
                        nome = it[Jogadores.nome],
                        posicao = it[Jogadores.posicao],
                        n_camisola = it[Jogadores.n_camisola],
                        id = it[Jogadores.id]
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun atualizarJogador(id: Int, dados: Jogador) {
        suspendTransaction(database) {
            Jogadores.update ({ Jogadores.id eq id }) {
                it[nome] = dados.nome
                it[posicao] = dados.posicao
                it[n_camisola] = dados.n_camisola
            }
        }
    }

    suspend fun removerJogador(id: Int) {
        suspendTransaction(database) {
            Jogadores.deleteWhere { Jogadores.id eq id }
        }
    }

}