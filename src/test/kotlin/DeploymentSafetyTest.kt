package com.est

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeploymentSafetyTest {
    @Test
    fun `allows local development environment`() {
        assertEquals(Unit, ensureDeploymentEnvironmentIsSupported("development"))
        assertEquals(Unit, ensureDeploymentEnvironmentIsSupported(null))
    }

    @Test
    fun `rejects production until route authentication exists`() {
        val exception = assertFailsWith<IllegalStateException> {
            ensureDeploymentEnvironmentIsSupported("production")
        }
        kotlin.test.assertTrue(exception.message!!.contains("autenticação e autorização"))
    }

    @Test
    fun `production check is case insensitive and ignores whitespace`() {
        assertFailsWith<IllegalStateException> {
            ensureDeploymentEnvironmentIsSupported(" Production ")
        }
    }
}
