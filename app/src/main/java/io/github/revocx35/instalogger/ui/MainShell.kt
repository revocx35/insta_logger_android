package io.github.revocx35.instalogger.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import io.github.revocx35.instalogger.ui.components.Wordmark
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.revocx35.instalogger.core.AppState
import io.github.revocx35.instalogger.core.Message
import io.github.revocx35.instalogger.core.MessageEdit
import io.github.revocx35.instalogger.core.IgState
import io.github.revocx35.instalogger.core.IgStatus
import io.github.revocx35.instalogger.core.UserRole
import io.github.revocx35.instalogger.data.AppGraph
import io.github.revocx35.instalogger.data.ServerSender
import io.github.revocx35.instalogger.data.ServerSession
import io.github.revocx35.instalogger.media.MediaOps
import io.github.revocx35.instalogger.media.MediaViewer
import io.github.revocx35.instalogger.ui.components.WaIcon
import io.github.revocx35.instalogger.ui.components.WaStatePill
import io.github.revocx35.instalogger.ui.components.WarnBanner
import io.github.revocx35.instalogger.ui.screens.AdminDeps
import io.github.revocx35.instalogger.ui.screens.AccountSwitcherDialog
import io.github.revocx35.instalogger.ui.screens.AdminScreen
import io.github.revocx35.instalogger.ui.screens.ChatListPane
import io.github.revocx35.instalogger.ui.screens.ChatListViewModel
import io.github.revocx35.instalogger.ui.screens.ChatPane
import io.github.revocx35.instalogger.ui.screens.ChatViewModel
import io.github.revocx35.instalogger.ui.screens.DeletedScreen
import io.github.revocx35.instalogger.ui.screens.DeletedViewModel
import io.github.revocx35.instalogger.ui.screens.NoChatSelected
import io.github.revocx35.instalogger.ui.screens.SearchScreen
import io.github.revocx35.instalogger.ui.screens.SearchViewModel
import io.github.revocx35.instalogger.ui.screens.SettingsDeps
import io.github.revocx35.instalogger.ui.screens.SettingsScreen
import io.github.revocx35.instalogger.ui.screens.InstagramScreen
import io.github.revocx35.instalogger.ui.screens.ReelsScreen
import io.github.revocx35.instalogger.ui.screens.ReelsViewModel
import io.github.revocx35.instalogger.ui.screens.StoriesScreen
import io.github.revocx35.instalogger.ui.screens.StoriesViewModel
import io.github.revocx35.instalogger.ui.theme.Wa
import io.github.revocx35.instalogger.ui.theme.WaIcons
import io.github.revocx35.instalogger.ui.theme.WaType
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object ChatsRoute
@Serializable data class ChatRoute(val chatId: String, val focus: String? = null)
@Serializable object DeletedRoute
@Serializable data class SearchRoute(val q: String = "")
@Serializable object InstagramRoute
@Serializable object ReelsRoute
@Serializable object StoriesRoute
@Serializable object SettingsRoute
@Serializable object AdminRoute

/**
 * [adminOnly] tabs exist only for the admin (the first account). [bottom] tabs fill Instagram's phone tab bar; the
 * others open from its "More" sheet.
 */
enum class Tab(val label: String, val icon: ImageVector, val adminOnly: Boolean = false, val bottom: Boolean = false) {
    Chats("Messages", WaIcons.chats, bottom = true), Search("Search", WaIcons.search, bottom = true), Reels("Reels", WaIcons.reels, bottom = true), Stories("Stories", WaIcons.stories, bottom = true), Deleted("Unsent", WaIcons.deleted), Instagram("Instagram", WaIcons.monitor), Admin("Users", WaIcons.group, adminOnly = true), Settings("Settings", WaIcons.settings)
}

private fun tabRoute(t: Tab): Any = when (t) {
    Tab.Chats -> ChatsRoute
    Tab.Deleted -> DeletedRoute
    Tab.Search -> SearchRoute()
    Tab.Reels -> ReelsRoute
    Tab.Stories -> StoriesRoute
    Tab.Instagram -> InstagramRoute
    Tab.Settings -> SettingsRoute
    Tab.Admin -> AdminRoute
}

private fun NavHostController.goTab(t: Tab) = navigate(tabRoute(t)) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** One icon of Instagram's tab bar / narrow sidebar: no label, the active one drawn bolder; [dot] = needs attention. */
@Composable
private fun RailItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Wa.colors.text, dot: Boolean = false) {
    Box(
        modifier.height(50.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Tab, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label; selected = active },
        contentAlignment = Alignment.Center,
    ) {
        Box {
            // The outline icons are 2 px strokes; Instagram marks the current tab with a filled/bolder icon.
            WaIcon(icon, 27.dp, tint, Modifier.then(if (active) Modifier.graphicsLayer(scaleX = 1.04f, scaleY = 1.04f) else Modifier))
            if (active) WaIcon(icon, 27.dp, tint, Modifier.padding(start = 0.6.dp, top = 0.6.dp))
            if (dot) Box(Modifier.align(Alignment.TopEnd).size(11.dp).clip(CircleShape).background(Wa.colors.panel).padding(2.dp).clip(CircleShape).background(Wa.colors.like))
        }
    }
}

/** Instagram's "More" sheet: the pages without a tab, the connection state and log out. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreSheet(tabs: List<Tab>, wa: IgStatus?, username: String?, onTab: (Tab) -> Unit, onLogout: () -> Unit, onAccounts: () -> Unit, onClose: () -> Unit) {
    val c = Wa.colors
    ModalBottomSheet(onDismissRequest = onClose, containerColor = c.sheet, dragHandle = { Box(Modifier.padding(top = 8.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(c.border)) }) {
        Column(Modifier.navigationBarsPadding().padding(8.dp)) {
            for (t in tabs) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClose(); onTab(t) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WaIcon(t.icon, 24.dp, c.text)
                    Text(t.label, style = WaType.base.copy(fontSize = 15.sp), color = c.text, modifier = Modifier.weight(1f))
                    if (t == Tab.Instagram) WaStatePill(wa, compact = true)
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClose(); onAccounts() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WaIcon(WaIcons.user, 24.dp, c.text)
                Text("Switch account", style = WaType.base.copy(fontSize = 15.sp), color = c.text)
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(6.dp).background(c.border))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClose(); onLogout() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WaIcon(WaIcons.logout, 24.dp, c.text)
                Text(if (username != null) "Log out @$username" else "Log out", style = WaType.base.copy(fontSize = 15.sp), color = c.text)
            }
        }
    }
}

/** Media actions for bubbles: viewer, save (system file picker), open with another app, retry, edits. */
@Composable
private fun rememberMediaActions(session: ServerSession, context: Context, onView: (Message) -> Unit): Pair<MessageActions, (Message) -> Unit> {
    val scope = rememberCoroutineScope()
    var pendingSave by remember { mutableStateOf<Message?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val msg = pendingSave
        pendingSave = null
        if (uri != null && msg != null) scope.launch { MediaOps.saveTo(context, session, msg, uri) }
    }
    val save: (Message) -> Unit = { msg ->
        pendingSave = msg
        saveLauncher.launch(MediaOps.defaultName(msg))
    }
    val actions = remember(session) {
        object : MessageActions {
            override fun openMedia(msg: Message) = onView(msg)
            override fun saveMedia(msg: Message) = save(msg)
            override fun saveToGallery(messages: List<Message>) {
                // Session scope: keeps going when the owner leaves the chat.
                session.scope.launch { MediaOps.saveAllToGallery(context.applicationContext, session.api, messages) }
            }
            override fun openDocument(msg: Message) {
                scope.launch { MediaOps.open(context, session, msg) }
            }
            override suspend fun retryMedia(messageId: String) {
                session.api.retryMedia(messageId)
            }
            override suspend fun edits(messageId: String): List<MessageEdit> = session.api.edits(messageId)
        }
    }
    return actions to save
}

/**
 * The logged-in app, laid out like the web UI: a menu rail (left on wide screens, bottom on phones,
 * hidden inside a chat) and a two-pane chat list + chat on wide screens.
 */
@Composable
fun MainShell(graph: AppGraph, session: ServerSession, state: AppState, wa: IgStatus?, onLogout: () -> Unit, onChangeServer: () -> Unit) {
    val c = Wa.colors
    val context = LocalContext.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination
    val inChat = dest?.hasRoute<ChatRoute>() == true
    val activeTab = when {
        dest == null -> Tab.Chats
        dest.hasRoute<ChatsRoute>() || inChat -> Tab.Chats
        dest.hasRoute<DeletedRoute>() -> Tab.Deleted
        dest.hasRoute<SearchRoute>() -> Tab.Search
        dest.hasRoute<ReelsRoute>() -> Tab.Reels
        dest.hasRoute<StoriesRoute>() -> Tab.Stories
        dest.hasRoute<InstagramRoute>() -> Tab.Instagram
        dest.hasRoute<SettingsRoute>() -> Tab.Settings
        dest.hasRoute<AdminRoute>() -> Tab.Admin
        else -> null
    }
    var viewing by remember { mutableStateOf<Message?>(null) }
    // The account menu: the open account, the others added on this phone, and "add another account".
    val accounts by graph.controller.accounts.collectAsState()
    val activeAccount by graph.controller.active.collectAsState()
    var showAccounts by remember { mutableStateOf(false) }
    val (actions, save) = rememberMediaActions(session, context) { viewing = it }
    val chatList: ChatListViewModel = viewModel(key = "chats:${session.id}") { ChatListViewModel(session) }
    val chatListState = rememberLazyListState()
    val showAdmin = state.role == UserRole.Admin
    val railTabs = Tab.entries.filter { showAdmin || !it.adminOnly }
    var moreOpen by remember { mutableStateOf(false) }
    val needsAttention = wa?.needsLogin == true
    val sender = remember(session) { ServerSender(context.applicationContext, session.api) }
    val sendBlocked = if (wa?.connected == true) null else "Instagram is not connected right now, so messages can’t be sent."

    fun openChat(id: String, focus: String? = null, replace: Boolean = false) {
        nav.navigate(ChatRoute(id, focus)) {
            if (replace) popUpTo<ChatsRoute>()
            launchSingleTop = focus == null
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(c.panel)) {
        val wide = maxWidth >= 900.dp

        val content: @Composable ColumnScope.() -> Unit = {
            if (needsAttention) {
                WarnBanner(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                    Text(
                        buildAnnotatedString {
                            append("Instagram is logged out — new messages are not being logged. ")
                            withStyle(SpanStyle(color = c.link, textDecoration = TextDecoration.Underline)) { append("Open the Instagram screen") }
                            append(" to fix it.")
                        },
                        style = WaType.small,
                        color = c.text,
                        modifier = Modifier.clickable { nav.goTab(Tab.Instagram) },
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().then(if (needsAttention) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier)) {
                CompositionLocalProviderActions(actions, sender) {
                    NavHost(nav, startDestination = ChatsRoute, modifier = Modifier.fillMaxSize()) {
                        composable<ChatsRoute> {
                            if (wide) {
                                Row(Modifier.fillMaxSize()) {
                                    ChatListPane(chatList, null, { openChat(it) }, { nav.navigate(SearchRoute(it)) }, Modifier.width(397.dp).fillMaxHeight(), chatListState, wa?.me?.username)
                                    Box(Modifier.width(1.dp).fillMaxHeight().background(c.border))
                                    NoChatSelected(Modifier.weight(1f).fillMaxHeight())
                                }
                            } else {
                                ChatListPane(chatList, null, { openChat(it) }, { nav.navigate(SearchRoute(it)) }, Modifier.fillMaxSize(), chatListState, wa?.me?.username)
                            }
                        }
                        composable<ChatRoute> { back ->
                            val r = back.toRoute<ChatRoute>()
                            val vm: ChatViewModel = viewModel(key = "chat:${session.id}:${r.chatId}:${r.focus}") { ChatViewModel(session, r.chatId, r.focus) }
                            if (wide) {
                                Row(Modifier.fillMaxSize()) {
                                    ChatListPane(chatList, r.chatId, { openChat(it, replace = true) }, { nav.navigate(SearchRoute(it)) }, Modifier.width(397.dp).fillMaxHeight(), chatListState, wa?.me?.username)
                                    Box(Modifier.width(1.dp).fillMaxHeight().background(c.border))
                                    ChatPane(vm, null, true, actions, Modifier.weight(1f).fillMaxHeight(), sendDisabledReason = sendBlocked)
                                }
                            } else {
                                ChatPane(vm, { nav.popBackStack() }, false, actions, Modifier.fillMaxSize(), sendDisabledReason = sendBlocked)
                            }
                        }
                        composable<DeletedRoute> {
                            val vm: DeletedViewModel = viewModel(key = "deleted:${session.id}") { DeletedViewModel(session) }
                            DeletedScreen(vm) { chatId, messageId -> openChat(chatId, messageId) }
                        }
                        composable<SearchRoute> { back ->
                            val r = back.toRoute<SearchRoute>()
                            val vm: SearchViewModel = viewModel(key = "search:${session.id}:${r.q}") { SearchViewModel(session, r.q) }
                            SearchScreen(vm.query, { vm.query = it }, vm.result, vm.error, vm.busy, vm::run) { chatId, messageId -> openChat(chatId, messageId) }
                        }
                        composable<InstagramRoute> { InstagramScreen(session.api, wa) }
                        composable<StoriesRoute> {
                            val vm: StoriesViewModel = viewModel(key = "stories:${session.id}") { StoriesViewModel(session) }
                            StoriesScreen(vm, session, wa?.connected == true) { nav.goTab(Tab.Settings) }
                        }
                        composable<ReelsRoute> {
                            val vm: ReelsViewModel = viewModel(key = "reels:${session.id}") { ReelsViewModel(session) }
                            ReelsScreen(vm, session, wa?.connected == true)
                        }
                        composable<AdminRoute> {
                            AdminScreen(AdminDeps(session.api, state.username, state.totpEnabled == true))
                        }
                        composable<SettingsRoute> {
                            SettingsScreen(
                                SettingsDeps(
                                    api = session.api,
                                    graph = graph,
                                    username = state.username,
                                    totpEnabled = state.totpEnabled == true,
                                    wa = wa,
                                    refresh = { graph.controller.refresh().join() },
                                    onChangeServer = onChangeServer,
                                    onOpenAdmin = if (showAdmin) ({ nav.goTab(Tab.Admin) }) else null,
                                    account = activeAccount,
                                    accounts = accounts,
                                    onOpenAccounts = { showAccounts = true },
                                    onAddAccount = { graph.controller.beginAddAccount() },
                                    onRemoveAccount = { graph.controller.removeAccount(it) },
                                ),
                            )
                        }
                    }
                }
            }
        }

        if (wide) {
            Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))) {
                SideRail(activeTab, wa, railTabs, { nav.goTab(it) }) { moreOpen = true }
                Column(Modifier.weight(1f).fillMaxHeight(), content = content)
            }
        } else {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))) {
                content()
                if (!inChat) BottomRail(activeTab, wa, railTabs.filter { it.bottom }, activeTab == Tab.Reels, { nav.goTab(it) }) { moreOpen = true }
            }
        }

        if (moreOpen) {
            // On phones the sheet lists every page without a tab; the wide rail already shows them all.
            MoreSheet(if (wide) emptyList() else railTabs.filter { !it.bottom }, wa, state.username, { nav.goTab(it) }, onLogout, { showAccounts = true }) { moreOpen = false }
        }
    }

    if (showAccounts) {
        AccountSwitcherDialog(
            accounts = accounts,
            active = activeAccount,
            onSwitch = { a -> showAccounts = false; graph.controller.switchTo(a.id) },
            onAdd = { showAccounts = false; graph.controller.beginAddAccount() },
            onRemove = { a -> graph.controller.removeAccount(a) },
            onClose = { showAccounts = false },
        )
    }

    viewing?.let { msg -> MediaViewer(session, msg, onSave = { save(msg) }, onClose = { viewing = null }) }
}

/** Instagram's narrow sidebar on wide screens: the wordmark's initials, one icon per page, "More" at the bottom. */
@Composable
fun SideRail(active: Tab?, wa: IgStatus?, tabs: List<Tab>, onTab: (Tab) -> Unit, onMore: () -> Unit) {
    val c = Wa.colors
    Row(Modifier.fillMaxHeight()) {
        Column(
            Modifier.width(72.dp).fillMaxHeight().background(c.rail).statusBarsPadding().navigationBarsPadding().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Wordmark(Modifier.padding(top = 8.dp, bottom = 16.dp), size = 28.sp, text = "Il")
                for (t in tabs) RailItem(t.label, t.icon, active == t, { onTab(t) }, dot = t == Tab.Instagram && wa?.connected != true)
            }
            RailItem("More", WaIcons.menu, false, onMore)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(c.border))
    }
}

/** Instagram's phone tab bar (hidden inside a chat; black on Reels): four tabs and "More". */
@Composable
fun BottomRail(active: Tab?, wa: IgStatus?, tabs: List<Tab>, dark: Boolean, onTab: (Tab) -> Unit, onMore: () -> Unit) {
    val c = Wa.colors
    val bg = if (dark) Color.Black else c.rail
    val fg = if (dark) Color.White else c.text
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(if (dark) Color(0xFF262626) else c.border))
        Row(Modifier.fillMaxWidth().background(bg).navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (t in tabs) RailItem(t.label, t.icon, active == t, { onTab(t) }, Modifier.weight(1f), tint = fg)
            RailItem("More", WaIcons.menu, active != null && !active.bottom, onMore, Modifier.weight(1f), tint = fg, dot = wa?.connected != true)
        }
    }
}

@Composable
private fun CompositionLocalProviderActions(actions: MessageActions, sender: MessageSender, content: @Composable () -> Unit) =
    androidx.compose.runtime.CompositionLocalProvider(LocalMessageActions provides actions, LocalSender provides sender, content = content)
