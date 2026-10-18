package io.github.revocx35.instalogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.revocx35.instalogger.core.ApiClient
import io.github.revocx35.instalogger.core.IgState
import io.github.revocx35.instalogger.core.IgStatus
import io.github.revocx35.instalogger.core.userMessage
import io.github.revocx35.instalogger.ui.components.BtnStyle
import io.github.revocx35.instalogger.ui.components.ConfirmButton
import io.github.revocx35.instalogger.ui.components.ErrorNote
import io.github.revocx35.instalogger.ui.components.MutedText
import io.github.revocx35.instalogger.ui.components.PageHeader
import io.github.revocx35.instalogger.ui.components.SwitchRow
import io.github.revocx35.instalogger.ui.components.WaButton
import io.github.revocx35.instalogger.ui.components.WaIcon
import io.github.revocx35.instalogger.ui.components.WaStatePill
import io.github.revocx35.instalogger.ui.components.WaTextField
import io.github.revocx35.instalogger.ui.components.WarnBanner
import io.github.revocx35.instalogger.ui.theme.Wa
import io.github.revocx35.instalogger.ui.theme.WaIcons
import io.github.revocx35.instalogger.ui.theme.WaType
import io.github.revocx35.instalogger.ui.vnc.VncView
import kotlinx.coroutines.launch

/** The real Instagram website in the background browser (like the web's Instagram page). */
@Composable
fun InstagramScreen(api: ApiClient, wa: IgStatus?) {
    val c = Wa.colors
    val scope = rememberCoroutineScope()
    val needsLink = wa?.needsLogin == true
    var viewOnly by rememberSaveable { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(needsLink) { if (needsLink) viewOnly = false }
    fun act(block: suspend () -> Unit) {
        error = null
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                error = e.userMessage()
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(c.panel)) {
        val side = if (maxWidth < 900.dp) 16.dp else minOf(maxWidth * 0.05f, 48.dp)
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = side, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageHeader(
                WaIcons.monitor,
                "Instagram",
                "The real Instagram website running in the background browser" +
                    (wa?.me?.let { me -> " — logged in as " + listOfNotNull(me.name, me.username?.let { "@$it" }).joinToString(" ") } ?: "") + ".",
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                WaStatePill(wa)
                SwitchRow(viewOnly, { viewOnly = it }, "View only")
                WaButton("Restart", { act { api.igRestart() } }, style = BtnStyle.Ghost, icon = WaIcons.refresh)
                ConfirmButton("Log out", "Log Instagram out", { act { api.igLogout() } }, style = BtnStyle.Ghost, enabled = wa?.connected == true)
            }
            if (needsLink) {
                WarnBanner(Modifier.clip(RoundedCornerShape(8.dp))) {
                    Text(if (wa?.state == IgState.Challenge) "Instagram asks for a confirmation. Answer it on the screen below." else "Instagram is logged out. Log in on the screen below.", style = WaType.small, color = c.text)
                }
                SessionImportPanel(api)
            }
            ErrorNote(error)
            VncView(api, viewOnly, Modifier.weight(1f).fillMaxWidth().heightIn(min = 300.dp))
            MutedText(
                "Tip: keep “View only” on to avoid accidental taps. Leave the tab on the inbox: an open conversation marks new messages as seen. The browser only allows Instagram and Meta domains; downloads and file uploads are disabled.",
                style = WaType.small,
            )
        }
    }
}

private fun steps(): List<androidx.compose.ui.text.AnnotatedString> {
    fun b(vararg parts: Pair<String, Boolean>) = buildAnnotatedString {
        for ((t, bold) in parts) if (bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(t) } else append(t)
    }
    return listOf(
        b("Tap into the Instagram page and type your " to false, "username" to true, " and " to false, "password" to true, " (keyboard button for typing)." to false),
        b("If Instagram asks for a " to false, "code" to true, " or to confirm it was you, answer it there too — or approve the login in the Instagram app." to false),
        b("Captcha keeps coming back? Use " to false, "Log in with your own browser" to true, " below the browser view." to false),
        b("Wait until the status says " to false, "Connected" to true, " or " to false, "Importing history" to true, ", then press " to false, "Complete" to true, "." to false),
    )
}

/** Onboarding step 3 (like the web's SetupLink page): live browser + status + Complete. */
@Composable
fun SetupLinkScreen(api: ApiClient, wa: IgStatus?, onComplete: suspend () -> Unit, onLogout: () -> Unit) {
    val c = Wa.colors
    val scope = rememberCoroutineScope()
    val state = wa?.state ?: IgState.Starting
    val linked = state == IgState.Ready || state == IgState.Syncing
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSteps by rememberSaveable { mutableStateOf(true) }

    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg)) {
        val wide = maxWidth >= 900.dp
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(if (wide) 20.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Log in to Instagram", style = WaType.authTitle, color = c.text)
                    MutedText("This is the real Instagram website running in a private browser on your server. Log in to it exactly like on a computer.", style = WaType.small)
                }
                WaStatePill(wa)
            }
            val stepList = @Composable {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    steps().forEachIndexed { i, s ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${i + 1}.", style = WaType.base, color = c.text)
                            Text(s, style = WaType.base, color = c.text)
                        }
                    }
                    MutedText("View-once photos/videos are never logged. Everything else you send or receive from now on is saved, encrypted, even if it is later unsent.", style = WaType.small)
                }
            }
            if (wide) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState())) { stepList() }
                    VncView(api, viewOnly = false, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.panel).clickable { showSteps = !showSteps }.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("How to log in", style = WaType.base.copy(fontWeight = FontWeight.Medium), color = c.text, modifier = Modifier.weight(1f))
                        WaIcon(WaIcons.chevronDown, 20.dp, c.text2)
                    }
                    if (showSteps) Column(Modifier.padding(top = 8.dp).heightIn(max = 260.dp).verticalScroll(rememberScrollState())) { stepList() }
                }
                VncView(api, viewOnly = false, modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 280.dp))
            }
            if (!linked) SessionImportPanel(api)
            val detail = listOfNotNull(wa?.detail, wa?.sync?.let { "${it.chatsDone}/${it.chatsTotal} chats" }).joinToString(" · ")
            if (detail.isNotEmpty()) MutedText(detail, style = WaType.small)
            ErrorNote(error)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                WaButton("Restart", {
                    error = null
                    scope.launch { runCatching { api.igRestart() }.onFailure { error = it.userMessage() } }
                }, style = BtnStyle.Ghost, icon = WaIcons.refresh)
                WaButton(if (linked) "Complete" else "Waiting for Instagram…", {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            onComplete()
                        } catch (e: Exception) {
                            error = e.userMessage()
                        } finally {
                            busy = false
                        }
                    }
                }, style = BtnStyle.Primary, enabled = linked, busy = busy)
                WaButton("Log out", onLogout, style = BtnStyle.Ghost, icon = WaIcons.logout)
            }
        }
    }
}

/**
 * "Log in with your own browser": paste DevTools' "Copy as cURL" of a request to instagram.com (or the Cookie header)
 * from a browser where you are logged in. The pasted text is a credential: sent once, then cleared.
 */
@Composable
fun SessionImportPanel(api: ApiClient) {
    val c = Wa.colors
    val scope = rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.panel2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text("Captcha keeps coming back? Log in with your own browser", style = WaType.base.copy(fontWeight = FontWeight.Medium), color = c.text, modifier = Modifier.weight(1f))
            WaIcon(WaIcons.chevronDown, 20.dp, c.text2)
        }
        if (open) {
            MutedText(
                "On a computer: open a private window, log in to instagram.com, press F12 → Network, reload, right-click the first request → Copy → Copy as cURL (bash). Paste it here. Then close the private window without logging out.",
                style = WaType.small,
            )
            WaTextField(text, { text = it; done = false }, "Copied request", singleLine = false, placeholder = "curl 'https://www.instagram.com/' … -b 'sessionid=…'")
            ErrorNote(error)
            if (done) Text("Login handed over. The status changes to Connected once Instagram accepts it.", style = WaType.small, color = c.accent2)
            WaButton("Use this login", {
                busy = true
                error = null
                scope.launch {
                    try {
                        api.igImportSession(text)
                        text = ""
                        done = true
                    } catch (e: Exception) {
                        error = e.userMessage()
                    } finally {
                        busy = false
                    }
                }
            }, style = BtnStyle.Primary, enabled = text.isNotBlank(), busy = busy)
        }
    }
}
