package com.est.Security

import io.ktor.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusinessRouteSecurityTest {
    @Test
    fun explicitlyPublicReadRoutesRemainPublic() {
        assertTrue(isPublicRead(HttpMethod.Get, "/eventos"))
        assertTrue(isPublicRead(HttpMethod.Get, "/eventos/12"))
        assertTrue(isPublicRead(HttpMethod.Get, "/eventos/12/chaveamento"))
        assertTrue(isPublicRead(HttpMethod.Get, "/classificacoes/geral"))
        assertTrue(isPublicRead(HttpMethod.Get, "/classificacoes/torneio/12"))
        assertTrue(isPublicRead(HttpMethod.Get, "/publico"))
        assertTrue(isPublicRead(HttpMethod.Get, "/hub"))
        assertTrue(isPublicRead(HttpMethod.Get, "/static/css/site.css"))
    }

    @Test
    fun mutationsAndPrivateResourcesAreNotPublic() {
        assertFalse(isPublicRead(HttpMethod.Post, "/eventos"))
        assertFalse(isPublicRead(HttpMethod.Put, "/eventos/12"))
        assertFalse(isPublicRead(HttpMethod.Delete, "/eventos/12"))
        assertFalse(isPublicRead(HttpMethod.Get, "/equipas"))
        assertFalse(isPublicRead(HttpMethod.Get, "/jogadores"))
        assertFalse(isPublicRead(HttpMethod.Get, "/inscricoes"))
        assertFalse(isPublicRead(HttpMethod.Get, "/estatisticas/7"))
        assertFalse(isPublicRead(HttpMethod.Get, "/organizador"))
        assertFalse(isPublicRead(HttpMethod.Get, "/patrocinadores"))
        assertFalse(isPublicRead(HttpMethod.Get, "/pavilhoes"))
    }
}
