package com.gramcall.ai.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PIN ka hash, server token aur URL encrypted storage mein. PIN kabhi plain text save nahi hota. */
class SecureStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context, "gramcall_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private var failures = 0
    private var lockedUntil = 0L

    fun hasPin() = prefs.contains(PIN_HASH)

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(PIN_HASH, Base64.encodeToString(hash(pin, salt), Base64.NO_WRAP))
            .apply()
    }

    fun clearPin() { prefs.edit().remove(PIN_HASH).remove(PIN_SALT).apply() }

    /** 5 galat try ke baad 30 second ruk jaata hai. */
    fun lockoutSecondsLeft(): Int {
        val left = lockedUntil - System.currentTimeMillis()
        return if (left <= 0) 0 else ((left + 999) / 1000).toInt()
    }

    fun verifyPin(pin: String): Boolean {
        if (lockoutSecondsLeft() > 0) return false
        val salt = prefs.getString(PIN_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val want = prefs.getString(PIN_HASH, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val ok = MessageDigest.isEqual(want, hash(pin, salt))
        if (ok) { failures = 0 } else if (++failures >= 5) { failures = 0; lockedUntil = System.currentTimeMillis() + 30_000 }
        return ok
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, 60_000, 256)).encoded

    // ---- cloud ----
    var serverUrl: String
        get() = prefs.getString(SERVER_URL, "") ?: ""
        set(v) { prefs.edit().putString(SERVER_URL, v.trim().trimEnd('/')).apply() }
    var token: String
        get() = prefs.getString(TOKEN, "") ?: ""
        set(v) { prefs.edit().putString(TOKEN, v).apply() }
    var cloudVersion: Int
        get() = prefs.getInt(CLOUD_VERSION, 0)
        set(v) { prefs.edit().putInt(CLOUD_VERSION, v).apply() }

    fun clearCloudSession() { prefs.edit().remove(TOKEN).remove(CLOUD_VERSION).apply() }

    private companion object {
        const val PIN_HASH = "pin_hash"
        const val PIN_SALT = "pin_salt"
        const val SERVER_URL = "server_url"
        const val TOKEN = "token"
        const val CLOUD_VERSION = "cloud_version"
    }
}
