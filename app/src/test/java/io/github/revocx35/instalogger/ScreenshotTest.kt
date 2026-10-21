package io.github.revocx35.instalogger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.revocx35.instalogger.core.ChatSummary
import io.github.revocx35.instalogger.core.DeletedFeedItem
import io.github.revocx35.instalogger.core.Formatter
import io.github.revocx35.instalogger.core.Message
import io.github.revocx35.instalogger.core.SearchHit
import io.github.revocx35.instalogger.core.SearchResponse
import io.github.revocx35.instalogger.ui.BottomRail
import io.github.revocx35.instalogger.ui.LocalFormatter
import io.github.revocx35.instalogger.ui.NoMessageActions
import io.github.revocx35.instalogger.ui.SideRail
import io.github.revocx35.instalogger.ui.Tab
import io.github.revocx35.instalogger.ui.screens.AuthActions
import io.github.revocx35.instalogger.ui.screens.ChatFilter
import io.github.revocx35.instalogger.ui.screens.ChatListPane
import io.github.revocx35.instalogger.ui.screens.ChatListState
import io.github.revocx35.instalogger.ui.screens.ChatPane
import io.github.revocx35.instalogger.ui.screens.ChatScroll
import io.github.revocx35.instalogger.ui.screens.ChatState
import io.github.revocx35.instalogger.ui.screens.DeletedScreen
import io.github.revocx35.instalogger.ui.screens.DeletedState
import io.github.revocx35.instalogger.ui.screens.LoginScreen
import io.github.revocx35.instalogger.ui.screens.LoginStep
import io.github.revocx35.instalogger.ui.screens.AdminDeps
import io.github.revocx35.instalogger.ui.screens.AdminPage
import io.github.revocx35.instalogger.core.AdminOverview
import io.github.revocx35.instalogger.core.AdminPort
import io.github.revocx35.instalogger.core.AdminUser
import io.github.revocx35.instalogger.core.ApiClient
import io.github.revocx35.instalogger.core.IgState
import io.github.revocx35.instalogger.core.UserRole
import io.github.revocx35.instalogger.core.UserStatus
import io.github.revocx35.instalogger.core.parseServerUrl
import okhttp3.OkHttpClient
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import io.github.revocx35.instalogger.ui.screens.SearchScreen
import io.github.revocx35.instalogger.ui.screens.SetupRecoveryScreen
import io.github.revocx35.instalogger.ui.screens.SignupScreen
import io.github.revocx35.instalogger.ui.theme.Wa
import io.github.revocx35.instalogger.ui.theme.WaTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

private class FakeChatList(override val chats: List<ChatSummary>?) : ChatListState {
    override val error: String? = null
    override var query by mutableStateOf("")
    override var filter by mutableStateOf(ChatFilter.All)
}

private class FakeChat(override val chat: ChatSummary?, override val messages: List<Message>, override val highlight: String? = null) : ChatState {
    override val chatId: String = chat?.id ?: "x"
    override val nextBefore: String? = null
    override val nextAfter: String? = null
    override val loading = false
    override val loadingOlder = false
    override var error: String? = null
    override var searchOpen = false
    override var searchQuery = ""
    override val hits: List<SearchHit>? = null
    override val scroll: SharedFlow<ChatScroll> = MutableSharedFlow()
    override fun loadOlder() = Unit
    override fun loadNewer() = Unit
    override fun jump(messageId: String) = Unit
    override fun runSearch() = Unit
}

private class FakeDeleted(override val items: List<DeletedFeedItem>?) : DeletedState {
    override val next: String? = "more"
    override val error: String? = null
    override val busy = false
    override fun load(more: Boolean) = Unit
}

/** Records what the UI asked to send (nothing is sent in screenshot tests). */
class FakeSender : io.github.revocx35.instalogger.ui.MessageSender {
    val calls = mutableListOf<String>()
    override suspend fun sendText(chatId: String, text: String, quotedId: String?) { calls += "text:$text:$quotedId" }
    override suspend fun sendFile(chatId: String, file: io.github.revocx35.instalogger.ui.OutgoingFile, kind: io.github.revocx35.instalogger.core.SendMediaKind, caption: String?, quotedId: String?) { calls += "file:$kind" }
    override suspend fun react(messageId: String, emoji: String) { calls += "react:$emoji" }
    override suspend fun unsend(messageId: String) { calls += "unsend" }
    override suspend fun edit(messageId: String, text: String) { calls += "edit:$text" }
    override suspend fun markRead(chatId: String) { calls += "read" }
    override suspend fun readReceipts(): Boolean = false
}

/** The phone rail has no Users tab (admins open it from Settings there). */
private val phoneTabs = Tab.entries.filter { it != Tab.Admin }

private val noAuth = AuthActions({ _, _, _ -> }, { _, _, _ -> LoginStep.Done }, { _, _, _ -> }, { _, _, _, _ -> })

/**
 * Renders the main screens with sample data (JVM, Robolectric native graphics) so the design can be
 * compared with the web UI's screenshots. Record with `./gradlew :app:recordRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            WaTheme(dark) {
                CompositionLocalProvider(LocalFormatter provides Formatter(Locale.US, SampleData.zone, use24h = false)) {
                    Box(Modifier.fillMaxSize().background(Wa.colors.bg)) { content() }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private val aliSummary get() = SampleData.chats.first { it.id == SampleData.ALI }
    private val groupSummary get() = SampleData.chats.first { it.id == SampleData.GROUP }

    @Test
    fun phoneChatList() = shot("phone_chats") {
        Column(Modifier.fillMaxSize()) {
            ChatListPane(FakeChatList(SampleData.chats), null, {}, {}, Modifier.weight(1f).fillMaxWidth())
            BottomRail(Tab.Chats, phoneTabs) {}
        }
    }

    @Test
    fun phoneChatListDark() = shot("phone_chats_dark", dark = true) {
        Column(Modifier.fillMaxSize()) {
            ChatListPane(FakeChatList(SampleData.chats), null, {}, {}, Modifier.weight(1f).fillMaxWidth())
            BottomRail(Tab.Chats, phoneTabs) {}
        }
    }

    @Test
    fun phoneChat() = shot("phone_chat_ali") {
        ChatPane(FakeChat(aliSummary, SampleData.aliChat), {}, false, NoMessageActions, Modifier.fillMaxSize())
    }

    @Test
    fun phoneChatDark() = shot("phone_chat_ali_dark", dark = true) {
        ChatPane(FakeChat(aliSummary, SampleData.aliChat), {}, false, NoMessageActions, Modifier.fillMaxSize())
    }

    @Test
    fun phoneChatComposer() = shot("phone_chat_composer") {
        CompositionLocalProvider(io.github.revocx35.instalogger.ui.LocalSender provides FakeSender()) {
            ChatPane(FakeChat(aliSummary, SampleData.aliChat), {}, false, NoMessageActions, Modifier.fillMaxSize())
        }
    }

    @Test
    fun phoneMessageMenu() = shot("phone_message_menu") {
        val msg = SampleData.aliChat.last { it.fromMe && it.text != null }.copy(ts = System.currentTimeMillis())
        io.github.revocx35.instalogger.ui.components.MessageMenu(msg, true, {}, {}, {}, {}, null, {})
    }

    @Test
    fun phoneChatSelecting() = shot("phone_chat_selecting") {
        val selection = io.github.revocx35.instalogger.ui.MessageSelection().apply { selectAll(SampleData.aliChat) }
        ChatPane(FakeChat(aliSummary, SampleData.aliChat), {}, false, NoMessageActions, Modifier.fillMaxSize(), selection)
    }

    @Test
    fun phoneGroup() = shot("phone_chat_group") {
        ChatPane(FakeChat(groupSummary, SampleData.groupChat, highlight = null), {}, false, NoMessageActions, Modifier.fillMaxSize())
    }

    @Test
    fun phoneDeleted() = shot("phone_deleted") {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { DeletedScreen(FakeDeleted(SampleData.deleted)) { _, _ -> } }
            BottomRail(Tab.Deleted, phoneTabs) {}
        }
    }

    @Test
    fun phoneSearch() = shot("phone_search") {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { SearchScreen("plan", {}, SearchResponse(SampleData.hits, false), null, false, {}, { _, _ -> }) }
            BottomRail(Tab.Search, phoneTabs) {}
        }
    }

    @Test
    fun phoneLogin() = shot("phone_login") { LoginScreen(noAuth, {}, {}) }

    @Test
    fun phoneSignupDark() = shot("phone_signup_dark", dark = true) { SignupScreen(noAuth) {} }

    @Test
    fun phoneRecoveryKey() = shot("phone_recovery_key") { SetupRecoveryScreen("ABCD-EFGH-IJKL-MNOP-QRST-UVWX-YZ23-4567-ABCD-EFGH-IJKL-MNOP-QR") {} }

    @Config(qualifiers = "w1280dp-h800dp-mdpi")
    @Test
    fun tabletTwoPane() = shot("tablet_two_pane") {
        Row(Modifier.fillMaxSize()) {
            SideRail(Tab.Chats, SampleData.ready, phoneTabs, {}, {})
            ChatListPane(FakeChatList(SampleData.chats), SampleData.ALI, {}, {}, Modifier.widthIn(min = 300.dp, max = 440.dp).width(400.dp).fillMaxHeight())
            Box(Modifier.width(1.dp).fillMaxHeight().background(Wa.colors.border))
            ChatPane(FakeChat(aliSummary, SampleData.aliChat), null, true, NoMessageActions, Modifier.weight(1f).fillMaxHeight())
        }
    }

    private fun adminDeps() = AdminDeps(ApiClient(OkHttpClient(), parseServerUrl("https://ig.example.com")!!), "owner", false)

    private fun adminOverview() = AdminOverview(
        currentUserId = 1,
        users = listOf(
            AdminUser(1, "owner", UserRole.Admin, port = null, status = UserStatus.Active, ig = IgState.Ready),
            AdminUser(2, "friend", UserRole.User, port = 8101, status = UserStatus.Pending, ig = IgState.Login),
        ),
        defaultPort = 8080,
        defaultOrigin = "https://ig.example.com",
        ports = listOf(AdminPort(8101, null, listOf("friend"))),
        availablePorts = listOf(8101, 8102),
        maxUsers = 10,
    )

    @Test
    fun adminUsersScreen() = shot("admin_users") {
        AdminPage(adminDeps(), adminOverview(), null, {}, emptyList(), null)
    }

    @Test
    fun adminUsersPhoneShowsUsersTabAndPort() {
        compose.setContent {
            WaTheme(dark = false) { AdminPage(adminDeps(), adminOverview(), null, {}, emptyList(), null) }
        }
        compose.onNodeWithText("Signed in as owner (admin)").assertExists()
        compose.onNodeWithText("friend").assertExists()
        compose.onNodeWithText("Waiting for first sign-in").assertExists()
        compose.onNodeWithText("Signs in at https://ig.example.com:8101", substring = true).assertExists()
    }

    @Test
    fun loginWithATemporaryPasswordAsksForANewOne() {
        val claimAuth = AuthActions(
            { _, _, _ -> },
            { _, _, _ -> LoginStep.Claim(false) },
            { _, _, _ -> },
            { _, _, _, _ -> },
        )
        compose.setContent { WaTheme(dark = false) { LoginScreen(claimAuth, {}, {}) } }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("friend")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("Temp-Password-Issued-By-Admin")
        compose.onNode(hasText("Log in") and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Choose your own password").assertExists()
        compose.onNodeWithText("Set password and continue").assertExists()
    }
}

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateValue(): T = collectAsState().value
