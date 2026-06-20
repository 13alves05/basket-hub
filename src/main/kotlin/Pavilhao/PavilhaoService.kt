package com.est.Pavilhao

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
class Pavilhao (
    val nome: String,
    val cidade: String,
    val localizacao: String,
    val capacidade: Int,
    val id: Int? = null
)

class PavilhaoService(val database: R2dbcDatabase) {

    object Pavilhoes : Table("pavilhoes") {
        val id = integer("id").autoIncrement()
        val nome = varchar("nome", 100)
        val cidade = varchar("cidade", 100)
        val localizacao = varchar("localizacao", 255)
        val capacidade = integer("capacidade")

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun createSchema() {
        suspendTransaction(database) {
            SchemaUtils.create(Pavilhoes)
        }
    }

    suspend fun criarPavilhao(p: Pavilhao) {
        suspendTransaction(database) {
            Pavilhoes.insert {
                it[nome] = p.nome
                it[cidade] = p.cidade
                it[localizacao] = p.localizacao
                it[capacidade] = p.capacidade
            }
        }
    }

    suspend fun lerTodos(): List<Pavilhao> {
        return suspendTransaction(database) {
            Pavilhoes.selectAll().map {
                Pavilhao(
                    nome = it[Pavilhoes.nome],
                    cidade = it[Pavilhoes.cidade],
                    localizacao = it[Pavilhoes.localizacao],
                    capacidade = it[Pavilhoes.capacidade],
                    id = it[Pavilhoes.id]
                )
            }.toList()
        }
    }

    suspend fun lerPorId(id: Int): Pavilhao? {
        return suspendTransaction(database) {
            Pavilhoes.selectAll()
                .where { Pavilhoes.id eq id }
                .map {
                    Pavilhao(
                        nome = it[Pavilhoes.nome],
                        cidade = it[Pavilhoes.cidade],
                        localizacao = it[Pavilhoes.localizacao],
                        capacidade = it[Pavilhoes.capacidade],
                        id = it[Pavilhoes.id]
                    )
                }.singleOrNull()
        }
    }

    suspend fun atualizarPavilhao(id: Int, dados: Pavilhao) {
        suspendTransaction(database) {
            Pavilhoes.update({ Pavilhoes.id eq id }) {
                it[nome] = dados.nome
                it[cidade] = dados.cidade
                it[localizacao] = dados.localizacao
                it[capacidade] = dados.capacidade
            }
        }
    }

    suspend fun removerPavilhao(id: Int) {
        suspendTransaction(database) {
            Pavilhoes.deleteWhere { Pavilhoes.id eq id }
        }
    }
}