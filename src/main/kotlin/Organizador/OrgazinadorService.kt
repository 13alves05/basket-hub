package com.est.Organizador

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
class Organizador(
    val nome: String,
    val contacto: String,
    val email: String,
    val id: Int? = null
)

class OrganizadorService(val database: R2dbcDatabase) {

    object Organizadores : Table("organizadores") {
        val id = integer("id").autoIncrement()
        val nome = varchar("nome", 100)
        val contacto = varchar("contacto", 20)
        val email = varchar("email", 150)

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun createSchema() {
        suspendTransaction(database) {
            SchemaUtils.create(Organizadores) // CORRIGIDO: Criar a tabela Organizadores!
        }
    }

    suspend fun criarOrganizador(o: Organizador) {
        suspendTransaction(database) {
            Organizadores.insert {
                it[nome] = o.nome
                it[contacto] = o.contacto
                it[email] = o.email
            }
        }
    }

    suspend fun lerTodos(): List<Organizador> {
        return suspendTransaction(database) {
            Organizadores.selectAll().map {
                Organizador(
                    nome = it[Organizadores.nome],
                    contacto = it[Organizadores.contacto],
                    email = it[Organizadores.email],
                    id = it[Organizadores.id]
                )
            }.toList()
        }
    }

    suspend fun lerPorId(id: Int): Organizador? {
        return suspendTransaction(database) {
            Organizadores.selectAll()
                .where { Organizadores.id eq id }
                .map {
                    Organizador(
                        nome = it[Organizadores.nome],
                        contacto = it[Organizadores.contacto],
                        email = it[Organizadores.email],
                        id = it[Organizadores.id]
                    )
                }.singleOrNull()
        }
    }

    suspend fun atualizar(id: Int, o: Organizador) {
        suspendTransaction(database) {
            Organizadores.update({ Organizadores.id eq id }) {
                it[nome] = o.nome
                it[contacto] = o.contacto
                it[email] = o.email
            }
        }
    }

    suspend fun remover(id: Int) {
        suspendTransaction(database) {
            Organizadores.deleteWhere { Organizadores.id eq id }
        }
    }
}