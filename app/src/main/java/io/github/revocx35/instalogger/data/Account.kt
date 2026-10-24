package io.github.revocx35.instalogger.data

import io.github.revocx35.instalogger.core.parseServerUrl
import kotlinx.serialization.Serializable

/**
 * One signed-in insta_logger server (or one user on it) kept on this phone. Everything secret —
 * the session cookie and the authentik sign-in — lives in files named after [id] (see
 * [SecureCookieJar] and [KeystoreAuthentikStore]), so switching accounts never mixes them up.
 *
 * [username] is only a label for the account menu; it is refreshed from `/api/state` after a
 * successful load and holds no secret.
 */
@Serializable
data class Account(
    val id: String,
    /** Normalized server address (`https://host:port/`), as [parseServerUrl] returns it. */
    val serverUrl: String,
    /** The owner explicitly agreed to plain HTTP for a public address (kept per account). */
    val allowHttp: Boolean = false,
    /** The user name of this account on that server, once known. */
    val username: String? = null,
    val addedAt: Long = 0L,
    val lastUsedAt: Long = 0L,
) {
    /** Host of the server, used for the account menu and the per-host certificate trust. */
    val host: String get() = parseServerUrl(serverUrl)?.host ?: serverUrl

    /** `host:port`, with the scheme's default port left out: two servers on one host stay tellable apart. */
    val hostPort: String
        get() = parseServerUrl(serverUrl)?.let {
            if (it.port == okhttp3.HttpUrl.defaultPort(it.scheme)) it.host else "${it.host}:${it.port}"
        } ?: serverUrl

    /** What the account menu shows as its title (the user name once the server told us). */
    val label: String get() = username?.takeIf { it.isNotBlank() } ?: hostPort

    /** The address without the trailing slash, for display. */
    val address: String get() = serverUrl.removeSuffix("/")

    companion object {
        /**
         * The account that carried the single saved server over from 0.5.0. Its secrets keep the
         * old file names, so an update does not sign the owner out.
         */
        const val MAIN = "main"
    }
}
