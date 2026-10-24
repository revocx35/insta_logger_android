package io.github.revocx35.instalogger.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.revocx35.instalogger.core.ApiException
import io.github.revocx35.instalogger.core.Authentik
import io.github.revocx35.instalogger.core.AuthentikRequiredException
import io.github.revocx35.instalogger.data.AppGraph
import io.github.revocx35.instalogger.data.AuthentikSignIn
import io.github.revocx35.instalogger.data.ServerSession
import io.github.revocx35.instalogger.ui.components.BtnStyle
import io.github.revocx35.instalogger.ui.components.ErrorNote
import io.github.revocx35.instalogger.ui.components.LinkButton
import io.github.revocx35.instalogger.ui.components.Note
import io.github.revocx35.instalogger.ui.components.WaButton
import io.github.revocx35.instalogger.ui.components.WaTextField
import io.github.revocx35.instalogger.ui.theme.Wa
import io.github.revocx35.instalogger.ui.theme.WaIcons
import io.github.revocx35.instalogger.ui.theme.WaType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.IOException

/** Where the proxy sent the request when [e] is "authentik sign-in needed". */
fun ApiException.authentikLocation(): HttpUrl? = (cause as? AuthentikRequiredException)?.location

/**
 * "Sign in with authentik" for a server behind authentik forward auth: opens authentik's login in the
 * browser, then [onSignedIn] (retry whatever needed the server). The client ID is the public OAuth client
 * set up in authentik for this app (README "authentik").
 *
 * [authentik] and [client] belong to the account being signed in (or the server being added), so each
 * account keeps its own authentik session.
 */
@Composable
fun AuthentikPanel(graph: AppGraph, authentik: Authentik, client: OkHttpClient, site: HttpUrl, location: HttpUrl?, onSignedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var clientId by rememberSaveable { mutableStateOf(authentik.tokens()?.clientId ?: AuthentikSignIn.DEFAULT_CLIENT_ID) }
    var showClientId by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Note("This server is protected by authentik. Sign in with your authentik account first; the server's own login comes after that.", WaIcons.shield)
    if (showClientId) WaTextField(clientId, { clientId = it.trim(); error = null }, "authentik client ID", enabled = !busy)
    ErrorNote(error)
    WaButton(
        if (busy) "Waiting for authentik…" else "Sign in with authentik",
        {
            busy = true
            error = null
            scope.launch {
                try {
                    AuthentikSignIn.signIn(context, authentik, client, site, location, clientId)
                    onSignedIn()
                } catch (e: CancellationException) {
                    if (!currentCoroutineContext().isActive) throw e
                } catch (e: IOException) {
                    error = e.message ?: "authentik sign-in failed."
                } catch (e: Exception) {
                    error = "authentik sign-in failed."
                } finally {
                    busy = false
                }
            }
        },
        style = BtnStyle.Primary,
        block = true,
        busy = busy,
    )
    if (busy) {
        LinkButton("Cancel", { AuthentikSignIn.cancel() }, style = WaType.small, color = Wa.colors.muted)
    } else {
        LinkButton(if (showClientId) "Hide client ID" else "authentik client ID…", { showClientId = !showClientId }, style = WaType.small, color = Wa.colors.muted)
    }
}

/** Full screen for the open account whose authentik sign-in is missing or ran out. */
@Composable
fun AuthentikScreen(graph: AppGraph, session: ServerSession, location: HttpUrl?, onSignedIn: () -> Unit, onChangeServer: () -> Unit, onAccounts: (() -> Unit)? = null) {
    AuthShell("Sign in with authentik", session.baseUrl.host) {
        AuthentikPanel(graph, session.authentik, session.api.http, session.baseUrl, location, onSignedIn)
        ServerFooter(onChangeServer, onAccounts)
    }
}
