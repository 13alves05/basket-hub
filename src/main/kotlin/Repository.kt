package com.est.Repository

interface Repository<T, ID> {
    suspend fun criar(item: T)
    suspend fun lerTodos(): List<T>
    suspend fun lerPorId(id: ID): T?
    suspend fun atualizar(id: ID, item: T)
    suspend fun remover(id: ID)
}