package io.github.revocx35.instalogger.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import io.github.revocx35.instalogger.core.ApiException
import io.github.revocx35.instalogger.core.Formatter
import io.github.revocx35.instalogger.data.AppGraph
import io.github.revocx35.instalogger.data.LocalNetwork
import io.github.revocx35.instalogger.data.rememberLocalNetworkRequest
import io.github.revocx35.instalogger.data.ServerSession
import io.github.revocx35.instalogger.media.AudioController
import io.github.revocx35.instalogger.ui.components.Spinner
import io.github.revocx35.instalogger.ui.screens.AuthActions
import io.github.revocx35.instalogger.ui.screens.AccountSwitcherDialog
import io.github.revocx35.instalogger.ui.screens.CertificateDialog
import io.github.revocx35.instalogger.ui.screens.LoginScreen
import io.github.revocx35.instalogger.ui.screens.LoginStep
import io.github.revocx35.instalogger.ui.screens.RecoverScreen
import io.github.revocx35.instalogger.ui.screens.ServerScreen
import io.github.revocx35.instalogger.ui.screens.SetupLinkScreen
import io.github.revocx35.instalogger.ui.screens.SetupRecoveryScreen
import io.github.revocx35.instalogger.ui.screens.SignupScreen
import io.github.revocx35.instalogger.ui.screens.UnreachableScreen
import io.github.revocx35.instalogger.ui.screens.AuthentikScreen
import io.github.revocx35.instalogger.ui.screens.authentikLocation
import io.github.revocx35.instalogger.ui.theme.Wa
import kotlinx.coroutines.launch
import java.security.cert.X509Certificate

private class SessionMediaHost(private val s: ServerSession) : MediaHost {
    override val imageLoader: ImageLoader get() = s.imageLoader
    override val audio: AudioController get() = s.audio
    override fun url(relative: String?): String? = s.mediaUrl(relative)
}

/**
 * Decides which screen the owner belongs on, like the web's route guard (`routeFor`):
 * server → signup → login/recover → recovery key → link Instagram → main.
 *
 * When another account is being added the server screen comes first and the open account stays in the
 * background; everything else belongs to the account that is being shown.
 */
@Composable
fun AppRoot(graph: AppGraph) {
    val ctrl = graph.controller
    val session by ctrl.session.collectAsState()
    val state by ctrl.state.collectAsState()
    val loading by ctrl.loading.collectAsState()
    val error by ctrl.error.collectAsState()
    val wa by ctrl.ig.collectAsState()
    val pendingKey by ctrl.pendingRecoveryKey.collectAsState()
    val adding by ctrl.adding.collectAsState()
    val accounts by ctrl.accounts.collectAsState()
    val activeAccount by ctrl.active.collectAsState()
    var showAccounts by remember { mutableStateOf(false) }
    val openAccounts: (() -> Unit)? = if (accounts.size > 1) ({ showAccounts = true }) else null
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val formatter = remember(config) { Formatter(config.locales[0], java.time.ZoneId.systemDefault(), DateFormat.is24HourFormat(context)) }
    var recoverMode by rememberSaveable { mutableStateOf(false) }
    var reviewCert by remember { mutableStateOf<X509Certificate?>(null) }

    // "Change server" on the sign-in screens forgets a server that was never signed in on (or adds one
    // when the open account works); "Add another account" always adds. The account menu switches.
    val changeServer: () -> Unit = { ctrl.changeServer() }

    // Android 17+: e.g. after a system update, a saved home-network server needs local network access first.
    var localNetworkMissing by remember { mutableStateOf(LocalNetwork.missing(context)) }
    val askLocalNetwork = rememberLocalNetworkRequest { granted ->
        localNetworkMissing = !granted
        if (granted) ctrl.refresh()
    }

    val s = session
    val host = remember(s) { s?.let { SessionMediaHost(it) } }
    CompositionLocalProvider(LocalFormatter provides formatter, LocalMediaHost provides host) {
        val st = state
        when {
            adding -> ServerScreen(graph, onCancel = { ctrl.cancelAddAccount() }) { url, http -> ctrl.connect(url, http) }
            s == null -> ServerScreen(graph) { url, http -> ctrl.connect(url, http) }
            st == null && loading -> Box(Modifier.fillMaxSize().background(Wa.colors.bg), contentAlignment = Alignment.Center) { Spinner(32.dp) }
            st == null && error?.code == ApiException.AUTHENTIK -> AuthentikScreen(
                graph,
                s,
                error?.authentikLocation(),
                onSignedIn = { ctrl.refresh() },
                onChangeServer = changeServer,
                onAccounts = openAccounts,
            )
            st == null -> UnreachableScreen(
                error?.userMessage,
                onRetry = { ctrl.refresh() },
                onChangeServer = changeServer,
                onReviewCertificate = if (error?.code == ApiException.TLS_UNTRUSTED && graph.trust.lastRejected != null) {
                    { reviewCert = graph.trust.lastRejected?.lastOrNull() }
                } else {
                    null
                },
                busy = loading,
                onAllowLocalNetwork = if (localNetworkMissing && error?.code == ApiException.NETWORK) askLocalNetwork else null,
                onAccounts = openAccounts,
            )
            else -> {
                val auth = remember(s) {
                    AuthActions(
                        signup = { token, user, pw ->
                            val res = s.api.signup(token, user, pw)
                            ctrl.pendingRecoveryKey.value = res.recoveryKey
                            ctrl.refresh().join()
                        },
                        login = { user, pw, totp ->
                            val res = s.api.login(user, pw, totp)
                            when {
                                !res.ok && res.mustChangePassword == true -> LoginStep.Claim(res.needRecoveryKey == true)
                                !res.ok && res.needTotp == true -> LoginStep.NeedTotp
                                else -> {
                                    ctrl.refresh().join()
                                    LoginStep.Done
                                }
                            }
                        },
                        recover = { user, key, pw ->
                            val res = s.api.recover(user, key, pw)
                            ctrl.pendingRecoveryKey.value = res.recoveryKey
                            ctrl.refresh().join()
                        },
                        claim = { user, tempPw, newPw, recoveryKey ->
                            val res = s.api.claim(user, tempPw, newPw, recoveryKey)
                            ctrl.pendingRecoveryKey.value = res.recoveryKey
                            ctrl.refresh().join()
                        },
                    )
                }
                val logout: () -> Unit = { scope.launch { ctrl.logout() } }
                when {
                    !st.hasOwner -> SignupScreen(auth, onAccounts = openAccounts, onChangeServer = changeServer)
                    !st.authenticated -> if (recoverMode) RecoverScreen(auth) { recoverMode = false } else LoginScreen(auth, { recoverMode = true }, onAccounts = openAccounts, onChangeServer = changeServer)
                    pendingKey != null -> SetupRecoveryScreen(pendingKey) { ctrl.pendingRecoveryKey.value = null; recoverMode = false }
                    !st.onboardingComplete -> SetupLinkScreen(s.api, wa, onComplete = { s.api.completeOnboarding(); ctrl.refresh().join() }, onLogout = logout)
                    // key(): the tabs, their navigation state and view models belong to one account.
                    else -> key(s.id) { MainShell(graph, s, st, wa, onLogout = logout, onChangeServer = changeServer) }
                }
            }
        }
    }

    // A crash or "not responding" since the last start: offer the report (stays on the phone unless copied).
    var report by remember { mutableStateOf(graph.diagnostics.unseen()) }
    report?.let {
        io.github.revocx35.instalogger.ui.components.ReportDialog(
            "insta_logger stopped unexpectedly",
            "Something went wrong the last time the app ran. The report below contains only technical details (no messages, names or passwords). Copy it and send it to whoever maintains your app.",
            it,
            onClose = { graph.diagnostics.markSeen(); report = null },
        )
    }

    val cert = reviewCert
    val sess = session
    if (cert != null && sess != null) {
        CertificateDialog(sess.baseUrl.host, cert, onTrust = {
            graph.trust.add(cert)
            reviewCert = null
            ctrl.refresh()
        }, onCancel = { reviewCert = null })
    }

    // The account menu from a sign-in screen (the main shell has its own copy inside the tabs).
    if (showAccounts) {
        AccountSwitcherDialog(
            accounts = accounts,
            active = activeAccount,
            onSwitch = { a -> showAccounts = false; ctrl.switchTo(a.id) },
            onAdd = { showAccounts = false; ctrl.beginAddAccount() },
            onRemove = { a -> ctrl.removeAccount(a) },
            onClose = { showAccounts = false },
        )
    }
}
