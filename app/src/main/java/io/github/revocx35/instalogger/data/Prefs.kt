package io.github.revocx35.instalogger.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Non-secret app settings: the accounts added on this phone (server address, HTTP choice, user name),
 * which one is active, certificates the owner chose to trust for a server host, the screenshot
 * protection switch and the auto-save settings. (Session cookies and authentik sign-ins are secrets and
 * live in [SecureCookieJar] and [KeystoreAuthentikStore] instead.)
 */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("wal_prefs", Context.MODE_PRIVATE)

    private val _secureScreen = MutableStateFlow(sp.getBoolean(KEY_SECURE_SCREEN, true))
    /** FLAG_SECURE: no screenshots, screen recording or previews in the app switcher. On by default. */
    val secureScreen: StateFlow<Boolean> = _secureScreen.asStateFlow()

    private val _accounts = MutableStateFlow(loadAccounts())
    /** Every account added on this phone, oldest first. */
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    /** The account the app is showing now. */
    var activeAccountId: String?
        get() = sp.getString(KEY_ACTIVE_ACCOUNT, null)
        set(v) = sp.edit().putString(KEY_ACTIVE_ACCOUNT, v).apply()

    fun saveAccounts(list: List<Account>) {
        // Oldest first: updating an account (its user name) must not move it in the account menu.
        val stable = list.sortedBy { it.addedAt }
        sp.edit().putString(KEY_ACCOUNTS, Json.encodeToString(ListSerializer(Account.serializer()), stable)).apply()
        _accounts.value = stable
    }

    /** Adds [account], or replaces the one with its id. */
    fun upsertAccount(account: Account) = saveAccounts(_accounts.value.filterNot { it.id == account.id } + account)

    fun removeAccount(id: String) = saveAccounts(_accounts.value.filterNot { it.id == id })

    /** Forgets that [id] was signed in; the account stays in the menu with its sign-in form. */
    fun clearUsername(id: String) = _accounts.value.firstOrNull { it.id == id }?.let { upsertAccount(it.copy(username = null)) }

    private fun loadAccounts(): List<Account> = runCatching {
        sp.getString(KEY_ACCOUNTS, null)?.let { Json.decodeFromString(ListSerializer(Account.serializer()), it) }
    }.getOrNull().orEmpty()

    fun setSecureScreen(on: Boolean) {
        sp.edit().putBoolean(KEY_SECURE_SCREEN, on).apply()
        _secureScreen.value = on
    }

    /**
     * 0.5.0 → 0.6.0: the one saved server becomes the first account ([Account.MAIN]) and the globally
     * trusted certificates become that server's. Runs once; the old keys are then removed.
     */
    fun migrateLegacyAccount(): Account? {
        if (sp.getBoolean(KEY_ACCOUNTS_READY, false)) return null
        val url = sp.getString(KEY_SERVER_LEGACY, null)?.takeIf { it.isNotBlank() }
        val account = url?.let {
            Account(Account.MAIN, it, sp.getBoolean(KEY_ALLOW_HTTP, false), null, System.currentTimeMillis(), System.currentTimeMillis())
        }
        if (account != null) {
            val certs = sp.getStringSet(KEY_CERTS, emptySet()).orEmpty()
            if (certs.isNotEmpty()) sp.edit().putStringSet(certKey(account.host), certs).apply()
            if (_accounts.value.none { it.id == account.id }) saveAccounts(listOf(account) + _accounts.value)
            activeAccountId = account.id
        }
        sp.edit().remove(KEY_SERVER_LEGACY).remove(KEY_ALLOW_HTTP).remove(KEY_CERTS).putBoolean(KEY_ACCOUNTS_READY, true).apply()
        return account
    }

    /** DER-encoded certificates trusted (in addition to Android's) for [host]. */
    fun trustedCerts(host: String?): List<ByteArray> {
        if (host.isNullOrBlank()) return emptyList()
        return sp.getStringSet(certKey(host), emptySet()).orEmpty().mapNotNull { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
    }

    fun setTrustedCerts(host: String, certs: List<ByteArray>) {
        sp.edit().putStringSet(certKey(host), certs.map { Base64.encodeToString(it, Base64.NO_WRAP) }.toSet()).apply()
    }

    /* "Save new photos to this phone" (media/AutoSave.kt) */
    var autoSave: Boolean
        get() = sp.getBoolean(KEY_AUTOSAVE, false)
        set(v) = sp.edit().putBoolean(KEY_AUTOSAVE, v).apply()
    var autoSaveVideos: Boolean
        get() = sp.getBoolean(KEY_AUTOSAVE_VIDEOS, false)
        set(v) = sp.edit().putBoolean(KEY_AUTOSAVE_VIDEOS, v).apply()
    var autoSaveStatus: Boolean
        get() = sp.getBoolean(KEY_AUTOSAVE_STATUS, false)
        set(v) = sp.edit().putBoolean(KEY_AUTOSAVE_STATUS, v).apply()
    var autoSaveOnlyDisappearing: Boolean
        get() = sp.getBoolean(KEY_AUTOSAVE_ONLY_DISAPPEARING, false)
        set(v) = sp.edit().putBoolean(KEY_AUTOSAVE_ONLY_DISAPPEARING, v).apply()
    var autoSaveCount: Int
        get() = sp.getInt(KEY_AUTOSAVE_COUNT, 0)
        set(v) = sp.edit().putInt(KEY_AUTOSAVE_COUNT, v).apply()
    var autoSaveLastRun: Long
        get() = sp.getLong(KEY_AUTOSAVE_LAST_RUN, 0)
        set(v) = sp.edit().putLong(KEY_AUTOSAVE_LAST_RUN, v).apply()
    var autoSaveError: String?
        get() = sp.getString(KEY_AUTOSAVE_ERROR, null)
        set(v) = sp.edit().putString(KEY_AUTOSAVE_ERROR, v).apply()

    /** The media-feed position, valid only for the server it was taken from. */
    fun autoSaveCursor(server: String): String? =
        if (sp.getString(KEY_AUTOSAVE_SERVER, null) == server) sp.getString(KEY_AUTOSAVE_CURSOR, null) else null

    fun setAutoSaveCursor(server: String?, cursor: String?) {
        sp.edit().putString(KEY_AUTOSAVE_SERVER, server).putString(KEY_AUTOSAVE_CURSOR, cursor).apply()
    }

    private fun certKey(host: String) = "certs:$host"

    private companion object {
        const val KEY_ACCOUNTS = "accounts"
        const val KEY_ACTIVE_ACCOUNT = "active_account"
        const val KEY_ACCOUNTS_READY = "accounts_ready"
        /** 0.5.0 and older: the one server this app talked to. */
        const val KEY_SERVER_LEGACY = "server_url"
        const val KEY_ALLOW_HTTP = "allow_http"
        const val KEY_CERTS = "trusted_certs"
        const val KEY_SECURE_SCREEN = "secure_screen"
        const val KEY_AUTOSAVE = "autosave"
        const val KEY_AUTOSAVE_VIDEOS = "autosave_videos"
        const val KEY_AUTOSAVE_STATUS = "autosave_status"
        const val KEY_AUTOSAVE_ONLY_DISAPPEARING = "autosave_only_disappearing"
        const val KEY_AUTOSAVE_COUNT = "autosave_count"
        const val KEY_AUTOSAVE_LAST_RUN = "autosave_last_run"
        const val KEY_AUTOSAVE_ERROR = "autosave_error"
        const val KEY_AUTOSAVE_SERVER = "autosave_server"
        const val KEY_AUTOSAVE_CURSOR = "autosave_cursor"
    }
}
