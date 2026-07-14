package com.networth.tracker.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

class PinPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isPinSet: Boolean
        get() = !prefs.getString(KEY_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_SALT, null).isNullOrBlank()

    fun setPin(pin: String) {
        require(isValidPin(pin)) { "PIN must be exactly 4 digits" }
        val salt = generateSalt()
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, hashPin(pin, salt))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (!isValidPin(pin) || !isPinSet) return false
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        return hashPin(pin, salt) == storedHash
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!verifyPin(currentPin) || !isValidPin(newPin)) return false
        setPin(newPin)
        return true
    }

    companion object {
        private const val PREFS_NAME = "pin_prefs"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val PIN_LENGTH = 4

        fun isValidPin(pin: String): Boolean =
            pin.length == PIN_LENGTH && pin.all { it.isDigit() }

        private fun generateSalt(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.toHex()
        }

        private fun hashPin(pin: String, salt: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
            return bytes.toHex()
        }

        private fun ByteArray.toHex(): String =
            joinToString("") { "%02x".format(it) }
    }
}
