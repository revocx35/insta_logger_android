package io.github.revocx35.instalogger.data

import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import io.github.revocx35.instalogger.BuildConfig
import io.github.revocx35.instalogger.core.ApiClient
import io.github.revocx35.instalogger.core.Authentik
import io.github.revocx35.instalogger.core.EventStream
import io.github.revocx35.instalogger.core.userMessage
import io.github.revocx35.instalogger.media.AudioController
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** App-wide singletons (created once in [io.github.revocx35.instalogger.InstaLoggerApp]). */
class AppGraph(val context: Context, val diagnostics: Diagnostics) {
    val prefs = Prefs(context)
    val trust = ServerTrust(prefs)
    /** Background work must never take the app down: record unexpected failures instead. */
    val errors = CoroutineExceptionHandler { ctx, e -> diagnostics.recordNonFatal(ctx[CoroutineName]?.name ?: "app", e) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + errors)

    private val userAgent = "insta_logger-android/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; ${Build.MANUFACTURER} ${Build.MODEL})"

    /** authentik's own endpoints: system trust, redirects followed, no server interceptors. */
    val authentikHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    /** Everything that talks to a server, before the account's cookie jar and authentik sign-in are added. */
    private val base: OkHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(trust.socketFactory, trust)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build()) }
        .build()

    /** The client for one account: its cookie jar plus its authentik sign-in interceptors. */
    fun serverClient(cookies: CookieJar, authentik: Authentik): OkHttpClient = base.newBuilder()
        .cookieJar(cookies)
        .addInterceptor(authentik.interceptor)
        .addNetworkInterceptor(authentik.networkInterceptor)
        .build()

    private val accountClients = ConcurrentHashMap<String, AccountClient>()

    /** Cookie jar, authentik sign-in and HTTP client of [accountId] (built once, then reused on switches). */
    fun accountClient(accountId: String): AccountClient = accountClients.getOrPut(accountId) { AccountClient(this, accountId) }

    /**
     * Deletes everything secret that belongs to [accountId] (session cookie, authentik sign-in) and drops
     * its client. Used when an account is removed or a server was added and then abandoned.
     */
    fun discardAccount(accountId: String) {
        accountClients.remove(accountId)
        SecureCookieJar(context, accountId).clear()
        KeystoreAuthentikStore(context, accountId).save(null)
    }

    /**
     * Secret files of accounts that no longer exist (e.g. the app was killed while adding a server).
     * Files of unknown origin are left alone; only the `session-<id>.bin` / `authentik-<id>.bin` names
     * this app writes are removed.
     */
    fun sweepOrphanSecrets() {
        val known = prefs.accounts.value.map { it.id }.toSet()
        val stale = context.noBackupFilesDir.listFiles().orEmpty().filter { f ->
            val n = f.name
            when {
                n.startsWith("session-") && n.endsWith(".bin") -> n.removePrefix("session-").removeSuffix(".bin") !in known
                n.startsWith("authentik-") && n.endsWith(".bin") -> n.removePrefix("authentik-").removeSuffix(".bin") !in known
                else -> false
            }
        }
        stale.forEach { runCatching { File(it.absolutePath).delete() } }
    }

    val autoSave = io.github.revocx35.instalogger.media.AutoSaver(context, prefs, scope, diagnostics)

    val controller = AppController(this)
}

/**
 * One account's secrets and HTTP client: its own encrypted cookie jar and its own authentik sign-in, so
 * two servers on this phone can never see each other's session. Kept for the lifetime of the process
 * (a switch back reuses it, still signed in).
 */
class AccountClient(graph: AppGraph, val accountId: String) {
    val cookies = SecureCookieJar(graph.context, accountId)
    val authentik = Authentik(KeystoreAuthentikStore(graph.context, accountId), graph.authentikHttp)
    val http: OkHttpClient = graph.serverClient(cookies, authentik)

    fun api(baseUrl: HttpUrl): ApiClient = ApiClient(http, baseUrl)
}

/**
 * Everything bound to one account: API client, live events, image loader. Replaced when the owner
 * switches accounts; the account's [AccountClient] (and so its sign-in) is reused.
 * Media is never written to a disk cache (memory cache only), so nothing decrypted outlives the process
 * except files the owner explicitly saves or opens.
 */
class ServerSession(graph: AppGraph, val account: Account, val baseUrl: HttpUrl) {
    val id: String = account.id
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + graph.errors)
    private val client = graph.accountClient(account.id)
    val api = client.api(baseUrl)
    val events = EventStream(api, scope)

    /** The account's authentik sign-in (the "Sign in with authentik" screen uses it). */
    val authentik: Authentik get() = client.authentik

    /** The account's encrypted session cookie jar. */
    val cookies: SecureCookieJar get() = client.cookies

    val imageLoader: ImageLoader = ImageLoader.Builder(graph.context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { api.http }))
            if (Build.VERSION.SDK_INT >= 28) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())
        }
        .memoryCache { MemoryCache.Builder().maxSizePercent(graph.context, 0.2).build() }
        .diskCache(null)
        .build()

    private var audioUsed = false

    /** Shared player for voice messages (created on first use). */
    val audio: AudioController by lazy {
        audioUsed = true
        AudioController(graph.context, api.http, scope)
    }

    private val diagnostics = graph.diagnostics

    /** UI text for a failed operation; unexpected (non-API) failures also go into the crash report. */
    fun fail(where: String, e: Throwable): String {
        if (e !is io.github.revocx35.instalogger.core.ApiException && e !is kotlinx.coroutines.CancellationException) diagnostics.recordNonFatal(where, e)
        return e.userMessage()
    }

    /** Absolute URL for a server-relative `/api/media/…` URL. */
    fun mediaUrl(relative: String?): String? = api.mediaUrl(relative)

    fun close() {
        events.stop()
        if (audioUsed) audio.release()
        imageLoader.memoryCache?.clear()
        imageLoader.shutdown()
        scope.cancel()
    }
}
