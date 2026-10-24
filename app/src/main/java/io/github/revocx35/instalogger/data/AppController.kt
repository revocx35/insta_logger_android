package io.github.revocx35.instalogger.data

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.revocx35.instalogger.core.ApiException
import io.github.revocx35.instalogger.core.AppState
import io.github.revocx35.instalogger.core.ServerEvent
import io.github.revocx35.instalogger.core.IgStatus
import io.github.revocx35.instalogger.core.parseServerUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import java.util.UUID

/**
 * App-level state, like the web's AppStateProvider: the server's `/api/state`, the live Instagram
 * status, and the one-time recovery key after signup/recovery (memory only, never persisted).
 * The live event stream runs only while the app is in the foreground and the owner is logged in.
 *
 * Multi-account: the phone keeps a list of accounts ([Account]) — a server address plus that account's
 * own encrypted session and authentik sign-in — and the app shows one at a time. Switching closes the
 * current session and opens the other one; nothing is signed out, so switching back is instant.
 */
class AppController(private val graph: AppGraph) {
    private val _session = MutableStateFlow<ServerSession?>(null)
    val session: StateFlow<ServerSession?> = _session.asStateFlow()

    /** Every account added on this phone (kept in [Prefs]). */
    val accounts: StateFlow<List<Account>> get() = graph.prefs.accounts

    private val _active = MutableStateFlow<Account?>(null)
    /** The account being shown. */
    val active: StateFlow<Account?> = _active.asStateFlow()

    private val _adding = MutableStateFlow(false)
    /** True while the owner is on the "add another server" screen (the current session stays open). */
    val adding: StateFlow<Boolean> = _adding.asStateFlow()

    /** The id the server being added will get; its secrets are already written under it. */
    private var pendingId: String? = null

    private val _state = MutableStateFlow<AppState?>(null)
    val state: StateFlow<AppState?> = _state.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Why the last `/api/state` failed (shown on the "cannot reach" screen). */
    private val _error = MutableStateFlow<ApiException?>(null)
    val error: StateFlow<ApiException?> = _error.asStateFlow()

    private val _ig = MutableStateFlow<IgStatus?>(null)
    val ig: StateFlow<IgStatus?> = _ig.asStateFlow()

    val pendingRecoveryKey = MutableStateFlow<String?>(null)

    private var inflight: Job? = null
    private var eventsJob: Job? = null
    private var foreground = false

    init {
        graph.prefs.migrateLegacyAccount()
        graph.sweepOrphanSecrets()
        val list = graph.prefs.accounts.value
        val start = list.firstOrNull { it.id == graph.prefs.activeAccountId } ?: list.firstOrNull()
        if (start != null) open(start) else _loading.value = false
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                foreground = true
                syncEvents()
                _session.value?.let { s ->
                    refresh()
                    graph.autoSave.kick(s.api)
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                foreground = false
                syncEvents()
            }
        })
    }

    /** The live stream runs only while the app is visible and the owner is logged in. */
    private fun syncEvents() {
        val s = _session.value ?: return
        if (foreground && _state.value?.authenticated == true) s.events.start() else s.events.stop()
    }

    /** Opens [account], closing the session that was open before (its secrets and client are kept). */
    private fun open(account: Account) {
        val url = parseServerUrl(account.serverUrl) ?: return
        inflight?.cancel()
        inflight = null
        _session.value?.close()
        _session.value = null
        eventsJob?.cancel()
        graph.trust.serverHost = account.host
        val s = ServerSession(graph, account, url)
        s.authentik.onSignInRequired = { graph.scope.launch { refresh() } }
        _session.value = s
        _active.value = account
        graph.prefs.activeAccountId = account.id
        graph.prefs.upsertAccount(account.copy(lastUsedAt = System.currentTimeMillis()))
        _state.value = null
        _ig.value = null
        _error.value = null
        eventsJob = graph.scope.launch {
            s.events.events.collect { ev ->
                when (ev) {
                    is ServerEvent.IgStateChanged -> _ig.value = ev.status
                    is ServerEvent.SyncProgressChanged -> _ig.value = _ig.value?.copy(sync = ev.sync)
                    is ServerEvent.SessionRevoked -> refresh()
                    is ServerEvent.MessageUpdate -> if (ev.reason == "media") graph.autoSave.kick(s.api)
                    else -> Unit
                }
            }
        }
        refresh()
    }

    /**
     * Switches to the account with [accountId] (from the account menu). The other account stays signed
     * in: its cookie and authentik sign-in are untouched.
     */
    fun switchTo(accountId: String) {
        val account = accounts.value.firstOrNull { it.id == accountId } ?: return
        if (_active.value?.id == account.id && _session.value != null) {
            refresh()
            return
        }
        dropPending()
        open(account)
    }

    /**
     * Starts adding a server: the "add another server" screen appears on top of the current account,
     * which stays open until the new one is connected.
     */
    fun beginAddAccount() {
        if (pendingId == null) pendingId = UUID.randomUUID().toString()
        _adding.value = true
    }

    /**
     * "Change server" on the sign-in and error screens: a server this app never signed in on is forgotten
     * first (a mistyped address leaves nothing behind), and the server screen comes back. Once an account
     * is signed in, a new one is added instead and this account stays in the account menu.
     */
    fun changeServer() {
        val cur = _active.value
        val neverSignedIn = cur != null && cur.username == null && _state.value?.authenticated != true
        if (!neverSignedIn) {
            beginAddAccount()
            return
        }
        val last = accounts.value.size <= 1
        removeAccount(cur!!)
        if (!last) beginAddAccount()
    }

    /** Leaves the "add another server" screen: the server that was being added is forgotten. */
    fun cancelAddAccount() {
        _adding.value = false
        dropPending()
        graph.trust.serverHost = _active.value?.host
    }

    /**
     * Where the new account's secrets go while the server is being checked (it also lets the owner sign
     * in with authentik before the account exists). The same id becomes the account's id.
     */
    fun pendingAccountId(): String = pendingId ?: UUID.randomUUID().toString().also { pendingId = it }

    /**
     * "Connect" on the server screen succeeded. A server already in the list is switched to instead of
     * added twice; otherwise the account is saved and opened (so a new server still needs its login).
     */
    fun connect(url: HttpUrl, allowHttp: Boolean) {
        val address = url.toString()
        val existing = accounts.value.firstOrNull { it.serverUrl == address }
        _adding.value = false
        if (existing != null) {
            dropPending()
            open(existing)
            return
        }
        val id = pendingId ?: UUID.randomUUID().toString()
        pendingId = null
        val now = System.currentTimeMillis()
        val account = Account(id = id, serverUrl = address, allowHttp = allowHttp, username = null, addedAt = now, lastUsedAt = now)
        graph.prefs.upsertAccount(account)
        open(account)
    }

    /** Removes an account from this phone: its session cookie, authentik sign-in and entry go away. */
    fun removeAccount(account: Account) {
        val wasActive = _active.value?.id == account.id || _session.value?.account?.id == account.id
        if (wasActive) {
            val s = _session.value
            inflight?.cancel()
            inflight = null
            eventsJob?.cancel()
            _session.value = null
            _state.value = null
            _ig.value = null
            _error.value = null
            s?.let { session ->
                // Sign out on the server too, so the cookie that is about to be deleted cannot be reused.
                graph.scope.launch(Dispatchers.IO) { runCatching { session.api.logout() } }
                session.close()
            }
            _active.value = null
        }
        graph.discardAccount(account.id)
        graph.prefs.removeAccount(account.id)
        if (wasActive) {
            val next = accounts.value.firstOrNull()
            if (next != null) {
                open(next)
            } else {
                graph.prefs.activeAccountId = null
                graph.trust.serverHost = null
                _loading.value = false
                pendingRecoveryKey.value = null
            }
        }
    }

    private fun dropPending() {
        val id = pendingId ?: return
        pendingId = null
        graph.discardAccount(id)
    }

    fun refresh(): Job {
        inflight?.let { if (it.isActive) return it }
        val s = _session.value ?: return Job().apply { complete() }
        _loading.value = true
        val job = graph.scope.launch {
            try {
                val st = s.api.state()
                if (_session.value !== s) return@launch
                s.api.csrfToken = st.csrfToken
                _state.value = st
                _error.value = null
                st.ig?.let { _ig.value = it }
                rememberUsername(s.account, st)
                syncEvents()
                if (!st.authenticated) s.cookies.clear()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (_session.value === s) {
                    _error.value = e as? ApiException ?: ApiException(0, "internal", s.fail("state", e), cause = e)
                    // Keep showing the last known state on transient errors once logged in.
                    // Without an authentik sign-in nothing works, so that one always leads to the sign-in screen.
                    if (_state.value?.authenticated != true || (e as? ApiException)?.code == ApiException.AUTHENTIK) _state.value = null
                }
            } finally {
                if (_session.value === s) _loading.value = false
            }
        }
        inflight = job
        return job
    }

    /** Keeps the account menu in step with what the server says (the user name, or that it signed out). */
    private fun rememberUsername(account: Account, st: AppState) {
        val name = st.username?.takeIf { it.isNotBlank() }
        if (!st.authenticated) {
            if (account.username != null) {
                graph.prefs.clearUsername(account.id)
                _active.value?.takeIf { it.id == account.id }?.let { _active.value = it.copy(username = null) }
            }
            return
        }
        if (name != null && name != account.username) {
            val updated = account.copy(username = name)
            graph.prefs.upsertAccount(updated)
            if (_active.value?.id == account.id) _active.value = updated
        }
    }

    suspend fun logout() {
        val s = _session.value ?: return
        runCatching { s.api.logout() }
        s.events.stop()
        s.cookies.clear()
        graph.prefs.clearUsername(s.account.id)
        refresh().join()
    }
}
