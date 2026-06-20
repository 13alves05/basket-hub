package com.est.Patrocinador

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import com.est.Evento.EventoService

@Serializable
data class Patrocinador(
    val id: Int? = null,
    val nome: String,
    val empresa: String,
    val valorContrato: Double,
    val eventoId: Int? = null
)

class PatrocinadorService(val database: R2dbcDatabase) {

    object Patrocinadores : Table("patrocinadores") {
        val id = integer("id").autoIncrement()
        val nome = varchar("nome", 100)
        val empresa = varchar("empresa", 100)
        val valorContrato = double("valor_contrato")

        // Foreign Key para Eventos (nullable porque pode nao patrocinar um evento especifico)
        val eventoId = reference("evento_id", EventoService.Eventos.id).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun createSchema() {
        suspendTransaction(database) {
            SchemaUtils.create(Patrocinadores)
        }
    }

    suspend fun criarPatrocinador(dados: Patrocinador) {
        suspendTransaction(database) {
            Patrocinadores.insert {
                it[nome] = dados.nome
                it[empresa] = dados.empresa
                it[valorContrato] = dados.valorContrato
                it[eventoId] = dados.eventoId
            }
        }
    }

    suspend fun lerPatrocinadores(): List<Patrocinador> {
        return suspendTransaction(database) {
            Patrocinadores.selectAll().map {
                Patrocinador(
                    id = it[Patrocinadores.id],
                    nome = it[Patrocinadores.nome],
                    empresa = it[Patrocinadores.empresa],
                    valorContrato = it[Patrocinadores.valorContrato],
                    eventoId = it[Patrocinadores.eventoId]
                )
            }.toList()
        }
    }

    suspend fun lerPatrocinadorPorId(id: Int): Patrocinador? {
        return suspendTransaction(database) {
            Patrocinadores.selectAll()
                .where { Patrocinadores.id eq id }
                .map {
                    Patrocinador(
                        id = it[Patrocinadores.id],
                        nome = it[Patrocinadores.nome],
                        empresa = it[Patrocinadores.empresa],
                        valorContrato = it[Patrocinadores.valorContrato],
                        eventoId = it[Patrocinadores.eventoId]
                    )
                }.singleOrNull()
        }
    }

    suspend fun atualizarPatrocinador(id: Int, dados: Patrocinador) {
        suspendTransaction(database) {
            Patrocinadores.update({ Patrocinadores.id eq id }) {
                it[nome] = dados.nome
                it[empresa] = dados.empresa
                it[valorContrato] = dados.valorContrato
                it[eventoId] = dados.eventoId
            }
        }
    }

    suspend fun removerPatrocinador(id: Int) {
        suspendTransaction(database) {
            Patrocinadores.deleteWhere { Patrocinadores.id eq id }
        }
    }
}