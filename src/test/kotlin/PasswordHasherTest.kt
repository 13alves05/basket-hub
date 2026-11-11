package com.est

import com.est.Security.PasswordHasher
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PasswordHasherTest {
    @Test
    fun `hash verifies the original password`() {
        val password = "BasketHub-Seguro-2026"
        val hash = PasswordHasher.hash(password)

        assertTrue(hash.startsWith("$" + "argon2id$"))
        assertTrue(PasswordHasher.verify(password, hash))
    }

    @Test
    fun `hash rejects a different password`() {
        val hash = PasswordHasher.hash("BasketHub-Seguro-2026")

        assertFalse(PasswordHasher.verify("Outra-Palavra-Passe-2026", hash))
    }

    @Test
    fun `hash rejects passwords shorter than twelve characters`() {
        assertFailsWith<IllegalArgumentException> {
            PasswordHasher.hash("curta")
        }
    }

    @Test
    fun `verify rejects a non Argon2id encoded value`() {
        assertFalse(PasswordHasher.verify("qualquer-palavra-passe", "not-a-password-hash"))
    }
}
