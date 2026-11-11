package com.est.Security

import de.mkammerer.argon2.Argon2Factory

/**
 * Argon2id password hashing. Plain-text passwords are only held in short-lived
 * character arrays and wiped after the operation.
 */
object PasswordHasher {
    private const val ITERATIONS = 3
    private const val MEMORY_KIB = 65_536
    private const val PARALLELISM = 1
    private const val MIN_PASSWORD_LENGTH = 12

    fun hash(password: String): String {
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "A palavra-passe deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres."
        }

        val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)
        val passwordChars = password.toCharArray()
        return try {
            argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, passwordChars)
        } finally {
            argon2.wipeArray(passwordChars)
        }
    }

    fun verify(password: String, encodedHash: String): Boolean {
        if (!encodedHash.startsWith("$" + "argon2id$")) return false

        val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)
        val passwordChars = password.toCharArray()
        return try {
            argon2.verify(encodedHash, passwordChars)
        } finally {
            argon2.wipeArray(passwordChars)
        }
    }
}
