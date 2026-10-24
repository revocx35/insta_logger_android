package io.github.revocx35.instalogger.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cookie jar for one account's insta_logger session cookie. The cookie unlocks the owner's private key
 * on the server, so it is only ever written to disk encrypted with a non-exportable AES-256-GCM key in
 * the Android Keystore (and the app excludes its data from backups).
 *
 * Every account has its own file ([accountId] in the name), so switching between accounts keeps each
 * server signed in without ever sending one server's cookie to another. [Account.MAIN] — the account
 * migrated from the single-server 0.5.0 builds — keeps the old file name, so an update does not sign
 * the owner out; the Keystore key is shared by all accounts.
 */
class SecureCookieJar(context: Context, accountId: String? = null) : CookieJar {
    private val file = File(context.noBackupFilesDir, fileName(accountId))
    private val cookies = ArrayList<Cookie>()
    private var loaded = false

    // Loaded on first use (a network thread), not at app start: Keystore + file I/O on the main
    // thread can take long enough on some phones to delay startup.
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching { load() }.onFailure { file.delete() }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        ensureLoaded()
        var changed = false
        for (c in cookies) {
            this.cookies.removeAll { it.name == c.name && it.domain == c.domain && it.path == c.path }
            if (c.expiresAt > System.currentTimeMillis()) this.cookies.add(c)
            changed = true
        }
        // An exception here would surface on OkHttp's thread and kill the app: keep the cookie in
        // memory only if the Keystore misbehaves.
        if (changed) runCatching { persist() }.onFailure { file.delete() }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        ensureLoaded()
        val now = System.currentTimeMillis()
        if (cookies.removeAll { it.expiresAt <= now }) runCatching { persist() }
        return cookies.filter { it.matches(url) }
    }

    @Synchronized
    fun clear() {
        loaded = true
        cookies.clear()
        file.delete()
    }

    @Serializable
    private data class Stored(val name: String, val value: String, val expiresAt: Long, val domain: String, val path: String, val secure: Boolean, val httpOnly: Boolean, val hostOnly: Boolean)

    private fun persist() {
        val persistent = cookies.filter { it.persistent }
        if (persistent.isEmpty()) {
            file.delete()
            return
        }
        val json = Json.encodeToString(ListSerializer(Stored.serializer()), persistent.map { Stored(it.name, it.value, it.expiresAt, it.domain, it.path, it.secure, it.httpOnly, it.hostOnly) })
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeBytes(cipher.iv + cipher.doFinal(json.toByteArray()))
        tmp.renameTo(file)
    }

    private fun load() {
        if (!file.exists()) return
        val data = file.readBytes()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data, 0, 12))
        val json = String(cipher.doFinal(data, 12, data.size - 12))
        val now = System.currentTimeMillis()
        for (s in Json.decodeFromString(ListSerializer(Stored.serializer()), json)) {
            if (s.expiresAt <= now) continue
            val b = Cookie.Builder().name(s.name).value(s.value).expiresAt(s.expiresAt).path(s.path)
            if (s.hostOnly) b.hostOnlyDomain(s.domain) else b.domain(s.domain)
            if (s.secure) b.secure()
            if (s.httpOnly) b.httpOnly()
            cookies.add(b.build())
        }
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    companion object {
        private const val ALIAS = "wal_session_cookie"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        /** `session.bin` for the account carried over from 0.5.0, `session-<id>.bin` for the others. */
        internal fun fileName(accountId: String?): String =
            if (accountId == null || accountId == Account.MAIN) "session.bin" else "session-${safe(accountId)}.bin"

        private fun safe(id: String) = id.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(64).ifEmpty { "account" }
    }
}
