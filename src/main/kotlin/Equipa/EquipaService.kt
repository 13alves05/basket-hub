package com.est.Equipa

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
class Equipa(
    val id: Int? = null,
    val nome: String,
    val sigla: String,
    val localidade: String
)

class EquipaService(val database: R2dbcDatabase) {

    object Equipas : Table("equipas") {
        val id = integer("id").autoIncrement()
        val nome = varchar("nome", 100)
        val sigla = varchar("sigla", 10)
        val localidade = varchar("localidade", 100)

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun createSchema() {
        suspendTransaction(database) {
            SchemaUtils.create(Equipas)
        }
    }

    suspend fun criarEquipa(dados: Equipa) {
        suspendTransaction(database) {
            Equipas.insert {
                it[nome] = dados.nome
                it[sigla] = dados.sigla
                it[localidade] = dados.localidade
            }
        }
    }

    suspend fun lerEquipas(): List<Equipa> {
        return suspendTransaction(database) {
            Equipas.selectAll().map {
                Equipa(
                    id = it[Equipas.id],
                    nome = it[Equipas.nome],
                    sigla = it[Equipas.sigla],
                    localidade = it[Equipas.localidade]
                )
            }.toList()
        }
    }

    suspend fun lerEquipaPorId(id: Int): Equipa? {
        return suspendTransaction(database) {
            Equipas.selectAll()
                .where { Equipas.id eq id }
                .map {
                    Equipa(
                        id = it[Equipas.id],
                        nome = it[Equipas.nome],
                        sigla = it[Equipas.sigla],
                        localidade = it[Equipas.localidade]
                    )
                }.singleOrNull()
        }
    }

    suspend fun atualizarEquipa(id: Int, dados: Equipa) {
        suspendTransaction(database) {
            Equipas.update({ Equipas.id eq id }) {
                it[nome] = dados.nome
                it[sigla] = dados.sigla
                it[localidade] = dados.localidade
            }
        }
    }

    suspend fun removerEquipa(id: Int) {
        suspendTransaction(database) {
            Equipas.deleteWhere { Equipas.id eq id }
        }
    }
}