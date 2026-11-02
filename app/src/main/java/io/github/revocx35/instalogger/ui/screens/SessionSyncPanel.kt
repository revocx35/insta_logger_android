package io.github.revocx35.instalogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.revocx35.instalogger.core.ApiClient
import io.github.revocx35.instalogger.core.IgState
import io.github.revocx35.instalogger.core.SyncHomeStatus
import io.github.revocx35.instalogger.core.SyncPairRequest
import io.github.revocx35.instalogger.core.SyncRemoteSave
import io.github.revocx35.instalogger.core.SyncRemoteStatus
import io.github.revocx35.instalogger.core.userMessage
import io.github.revocx35.instalogger.ui.components.BtnStyle
import io.github.revocx35.instalogger.ui.components.ConfirmButton
import io.github.revocx35.instalogger.ui.components.ErrorNote
import io.github.revocx35.instalogger.ui.components.MutedText
import io.github.revocx35.instalogger.ui.components.SwitchRow
import io.github.revocx35.instalogger.ui.components.WaButton
import io.github.revocx35.instalogger.ui.components.WaIcon
import io.github.revocx35.instalogger.ui.components.WaTextField
import io.github.revocx35.instalogger.ui.theme.Wa
import io.github.revocx35.instalogger.ui.theme.WaIcons
import io.github.revocx35.instalogger.ui.theme.WaType
import kotlinx.coroutines.launch

internal val SYNC_HOME_RESULT = mapOf(
    "given" to "a session was given",
    "challenge" to "Instagram asked for a check — log in on the screen yourself",
    "failed" to "Instagram did not accept the saved password",
    "no_form" to "the login form was not on the screen",
    "covered" to "something covered the login form",
    "busy" to "the screen was in use",
    "error" to "the browser could not be reached",
    "no_password" to "no saved password (Automatic login)",
    "cooldown" to "asked again too soon",
    "paused" to "paused after failed logins",
)

internal val SYNC_REMOTE_RESULT = mapOf(
    "logged_in" to "logged in with a session from the home",
    "unreachable" to "the home address did not answer",
    "refused" to "the home did not accept the pairing — pair again",
    "home_busy" to "the home was busy",
    "home_cooldown" to "the home asks to wait (one session per 15 minutes)",
    "home_paused" to "the home paused after failed logins",
    "home_no_password" to "the home has no saved password (Automatic login there)",
    "home_challenge" to "Instagram asked the home for a check",
    "home_failed" to "the home could not log in",
    "not_taken" to "the browser did not take the session",
    "error" to "something went wrong",
)

private fun whenText(t: Long?): String = t?.let { java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it)) } ?: ""

/**
 * Session sync (like the web's SessionSyncPanel): give separate Instagram sessions to a paired insta_logger that can't
 * log in itself (e.g. on a VPS), and/or get sessions from one that can. The pairing code is shown once, write-only.
 */
@Composable
fun SessionSyncPanel(api: ApiClient, igState: IgState?, totpEnabled: Boolean) {
    val c = Wa.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var home by remember { mutableStateOf<SyncHomeStatus?>(null) }
    var remote by remember { mutableStateOf<SyncRemoteStatus?>(null) }
    var open by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var asking by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var totp by remember { mutableStateOf("") }
    var code by remember { mutableStateOf<String?>(null) }
    var url by remember { mutableStateOf("") }
    var pasted by remember { mutableStateOf("") }

    suspend fun load() {
        runCatching { api.syncHome() }.onSuccess { home = it }.onFailure { error = (it as? Exception)?.userMessage() }
        runCatching { api.syncRemote() }.onSuccess { remote = it }
    }
    LaunchedEffect(igState) { load() }
    LaunchedEffect(home?.running, remote?.running) {
        while (home?.running == true || remote?.running == true) {
            kotlinx.coroutines.delay(3000)
            load()
        }
    }
    fun run(done: String? = null, block: suspend () -> Unit) {
        busy = true
        error = null
        note = null
        scope.launch {
            try {
                block()
                if (done != null) note = done
            } catch (e: Exception) {
                error = e.userMessage()
            } finally {
                busy = false
            }
        }
    }

    val h = home
    val r = remote
    val summary = when {
        r?.configured == true -> "getting sessions"
        h?.paired == true -> "giving sessions"
        h == null -> ""
        else -> "not set up"
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.panel2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text("Session sync" + if (summary.isNotEmpty()) " — $summary" else "", style = WaType.base.copy(fontWeight = FontWeight.Medium), color = c.text, modifier = Modifier.weight(1f))
            WaIcon(WaIcons.chevronDown, 20.dp, c.text2)
        }
        if (!open) return@Column
        MutedText(
            "For a second insta_logger with the same Instagram account that can't log in itself — Instagram hides the login form from datacenter addresses. The one that can log in (“home”) makes a separate login when the other one (“remote”) is logged out, and hands it over encrypted.",
            style = WaType.small,
        )
        ErrorNote(error)
        note?.let { Text(it, style = WaType.small, color = c.accent2) }

        // Giving side.
        Text("Give sessions (this is the home)", style = WaType.small.copy(fontWeight = FontWeight.Medium), color = c.text)
        if (h?.paired == true) {
            Text("Paired since ${whenText(h.pairedAt)}." + if (h.running) " Logging in for the remote…" else "", style = WaType.small, color = c.text)
        } else {
            MutedText("Not paired. Create a pairing code and paste it on the other insta_logger.", style = WaType.small)
        }
        if (h != null && !h.passwordSaved) Text("Save your Instagram login under “Automatic login” first: it makes the separate logins.", style = WaType.small, color = c.warn)
        h?.pausedUntil?.let { Text("Paused until ${whenText(it)} after failed logins.", style = WaType.small, color = c.warn) }
        if (h?.lastRequestAt != null && h.lastResult != null) MutedText("Last request ${whenText(h.lastRequestAt)}: ${SYNC_HOME_RESULT[h.lastResult] ?: h.lastResult}.", style = WaType.small)
        code?.let { shown ->
            MutedText("Paste this code on the other insta_logger (Instagram → Session sync → Get sessions). It is shown only now; it lets that insta_logger get sessions of this account.", style = WaType.small)
            SelectionContainer { Text(shown, style = WaType.mono, color = c.text) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WaButton("Copy", { copySensitive(context, "Pairing code", shown) }, style = BtnStyle.Ghost)
                WaButton("Done", { code = null }, style = BtnStyle.Ghost)
            }
        }
        if (asking) {
            WaTextField(password, { password = it }, "Your insta_logger password", password = true)
            if (totpEnabled) WaTextField(totp, { totp = it.filter { ch -> ch.isDigit() } }, "Authentication code")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WaButton(if (h?.paired == true) "Replace pairing" else "Create pairing code", {
                    run {
                        val p = api.syncPair(SyncPairRequest(password, totp.trim().ifEmpty { null }))
                        password = ""
                        totp = ""
                        asking = false
                        code = p.code
                        home = api.syncHome()
                    }
                }, style = BtnStyle.Primary, enabled = password.isNotEmpty(), busy = busy)
                WaButton("Cancel", { asking = false; password = ""; totp = "" }, style = BtnStyle.Ghost)
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WaButton(if (h?.paired == true) "New pairing code" else "Create pairing code", { asking = true }, enabled = !busy)
                if (h?.pausedUntil != null) WaButton("Allow again", { run("Allowed again.") { home = api.syncResume() } }, enabled = !busy)
                if (h?.paired == true) ConfirmButton("Unpair", "Remove the pairing", { run("Pairing removed.") { home = api.syncUnpair() } }, style = BtnStyle.Ghost)
            }
        }

        // Receiving side.
        Text("Get sessions (this is the remote)", style = WaType.small.copy(fontWeight = FontWeight.Medium), color = c.text, modifier = Modifier.padding(top = 6.dp))
        if (r?.configured == true) {
            Text("From ${r.url ?: ""}", style = WaType.small, color = c.text)
            SwitchRow(r.enabled, { on -> run { remote = api.saveSyncRemote(SyncRemoteSave(enabled = on)) } }, "Get a session automatically when logged out")
            if (r.running) MutedText("Asking the home…", style = WaType.small)
            if (r.paused) Text("Paused after failed tries. Check the home, save again or tap “Get session now”.", style = WaType.small, color = c.warn)
            if (r.lastAttemptAt != null && r.lastResult != null) MutedText("Last try ${whenText(r.lastAttemptAt)}: ${SYNC_REMOTE_RESULT[r.lastResult] ?: r.lastResult}.", style = WaType.small)
        }
        WaTextField(url, { url = it }, "Home address, e.g. http://ipvis-vpn:8091")
        WaTextField(pasted, { pasted = it }, "Pairing code from the home (igls1.…)", password = true)
        MutedText("The code is stored encrypted on the server and never shown again. The session travels encrypted end to end, also over plain HTTP.", style = WaType.small)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WaButton(if (r?.configured == true) "Replace" else "Save", {
                run("Saved.") {
                    remote = api.saveSyncRemote(SyncRemoteSave(url.trim(), pasted.trim(), true))
                    url = ""
                    pasted = ""
                }
            }, style = BtnStyle.Primary, enabled = url.isNotBlank() && pasted.isNotBlank(), busy = busy)
            if (r?.configured == true && (igState == IgState.Login || igState == IgState.Challenge)) {
                WaButton("Get session now", {
                    run {
                        val res = api.runSyncRemote()
                        note = when (res.result) {
                            "started" -> "Asking the home — it logs in on its own screen, this takes about a minute."
                            "not_on_login_page" -> "Instagram is not logged out here."
                            "busy" -> "Already asking."
                            else -> "Save the home address and pairing code first."
                        }
                        remote = api.syncRemote()
                    }
                }, enabled = !busy && !r.running)
            }
            if (r?.configured == true) ConfirmButton("Forget", "Forget the home", { run("Forgotten.") { remote = api.removeSyncRemote() } }, style = BtnStyle.Ghost)
        }
    }
}
