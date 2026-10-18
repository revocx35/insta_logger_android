package io.github.revocx35.instalogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import io.github.revocx35.instalogger.core.StoryAuthor
import io.github.revocx35.instalogger.core.StoryItem
import io.github.revocx35.instalogger.core.StoryStatus
import io.github.revocx35.instalogger.data.ServerSession
import io.github.revocx35.instalogger.media.buildPlayer
import io.github.revocx35.instalogger.ui.LocalFormatter
import io.github.revocx35.instalogger.ui.components.Avatar
import io.github.revocx35.instalogger.ui.components.BtnStyle
import io.github.revocx35.instalogger.ui.components.EmptyState
import io.github.revocx35.instalogger.ui.components.ErrorNote
import io.github.revocx35.instalogger.ui.components.MutedText
import io.github.revocx35.instalogger.ui.components.Spinner
import io.github.revocx35.instalogger.ui.components.WaButton
import io.github.revocx35.instalogger.ui.components.WaIcon
import io.github.revocx35.instalogger.ui.theme.Wa
import io.github.revocx35.instalogger.ui.theme.WaIcons
import io.github.revocx35.instalogger.ui.theme.WaType
import kotlinx.coroutines.launch

private val RING = Brush.linearGradient(listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5)))

/** Logged stories (`/api/stories…`): status, authors, items. */
class StoriesViewModel(private val session: ServerSession) : ViewModel() {
    var status by mutableStateOf<StoryStatus?>(null)
        private set
    var authors by mutableStateOf<List<StoryAuthor>>(emptyList())
        private set
    val items = mutableStateListOf<StoryItem>()
    var user by mutableStateOf<String?>(null)
        private set
    var next by mutableStateOf<Long?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var checking by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                status = session.api.storyStatus()
                authors = session.api.storyAuthors()
            } catch (e: Exception) {
                error = session.fail("stories", e)
            }
        }
        load(user, null)
    }

    fun select(u: String?) {
        user = u
        load(u, null)
    }

    fun more() = next?.let { load(user, it) }

    private fun load(u: String?, before: Long?) {
        loading = true
        error = null
        viewModelScope.launch {
            try {
                val page = session.api.stories(u, before)
                if (before == null) items.clear()
                items.addAll(page.items)
                next = page.nextBefore
            } catch (e: Exception) {
                error = session.fail("stories", e)
            } finally {
                loading = false
            }
        }
    }

    fun checkNow() {
        checking = true
        error = null
        viewModelScope.launch {
            try {
                status = session.api.storyCheck()
                refresh()
            } catch (e: Exception) {
                error = session.fail("stories", e)
            } finally {
                checking = false
            }
        }
    }
}

@Composable
fun StoriesScreen(vm: StoriesViewModel, session: ServerSession, connected: Boolean, onSettings: () -> Unit) {
    val c = Wa.colors
    val fmt = LocalFormatter.current
    var open by remember { mutableStateOf<Int?>(null) }
    val status = vm.status
    LazyVerticalGrid(
        GridCells.Adaptive(110.dp),
        Modifier.fillMaxSize().background(c.panel).statusBarsPadding(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Stories", style = WaType.paneTitle, color = c.text, modifier = Modifier.weight(1f))
                    if (status?.enabled == true) {
                        WaButton("Check now", { vm.checkNow() }, style = BtnStyle.Ghost, icon = WaIcons.refresh, busy = vm.checking, enabled = connected)
                    } else {
                        WaButton("Turn on", onSettings, style = BtnStyle.Primary)
                    }
                }
                MutedText(
                    when {
                        status == null -> ""
                        !status.enabled -> "Story logging is off. Turn it on in Settings → Logging."
                        else -> "Kept ${status.retentionDays} days · ${status.stories} saved" +
                            (status.lastCheckAt?.let { " · last check ${fmt.listTime(it)}" } ?: "") + (status.lastResult?.let { " ($it)" } ?: "")
                    },
                    style = WaType.small,
                )
                ErrorNote(vm.error)
            }
        }
        if (vm.authors.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    item { AuthorChip(null, "All", null, vm.user == null) { vm.select(null) } }
                    items(vm.authors, key = { it.id }) { a ->
                        AuthorChip(a, a.username ?: a.id, a.avatarUrl, vm.user == a.id) { vm.select(a.id) }
                    }
                }
            }
        }
        itemsIndexed(vm.items, key = { _, s -> s.id }) { i, s ->
            Box(Modifier.aspectRatio(9f / 16f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF111111)).clickable { open = i }) {
                if (s.url != null && s.type == "image") {
                    AsyncImage(session.mediaUrl(s.url), null, imageLoader = session.imageLoader, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (s.url == null) {
                    Text(if (s.status == "too_large") "Too large" else if (s.status == "pending") "Downloading…" else "Not saved", style = WaType.small, color = Color(0xFFBBBBBB), modifier = Modifier.align(Alignment.Center))
                }
                if (s.type == "video") WaIcon(WaIcons.play, 28.dp, Color.White, Modifier.align(Alignment.Center))
                Column(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000)))).padding(6.dp),
                ) {
                    Text(s.username ?: s.userId, style = WaType.tiny.copy(fontWeight = FontWeight.SemiBold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(fmt.listTime(s.takenAt), style = WaType.tiny, color = Color(0xDDFFFFFF))
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (vm.loading) Spinner()
                if (!vm.loading && vm.items.isEmpty()) {
                    EmptyState(WaIcons.stories, "No stories yet") {
                        MutedText(if (status?.enabled == true) "New stories of the accounts you follow appear here after the next check." else "Turn on story logging in Settings → Logging.")
                    }
                }
                if (!vm.loading && vm.next != null) WaButton("Older stories", { vm.more() }, style = BtnStyle.Ghost)
            }
        }
    }
    open?.let { i -> if (i in vm.items.indices) StoryViewer(vm.items, i, session, { open = it }) { open = null } }
}

@Composable
private fun AuthorChip(a: StoryAuthor?, label: String, avatar: String?, selected: Boolean, onClick: () -> Unit) {
    val c = Wa.colors
    Column(Modifier.width(68.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(62.dp).clip(CircleShape).background(RING).padding(3.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().clip(CircleShape).border(2.dp, c.panel, CircleShape), contentAlignment = Alignment.Center) {
                if (a == null) WaIcon(WaIcons.stories, 26.dp, Color.White) else Avatar(a.id, label, avatar, size = 54.dp)
            }
        }
        Text(label, style = WaType.tiny.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (a != null) MutedText("${a.count}", style = WaType.tiny)
    }
}

/** Full-screen viewer: tap left/right for previous/next, videos play and move on when they end. */
@Composable
private fun StoryViewer(items: List<StoryItem>, index: Int, session: ServerSession, onIndex: (Int) -> Unit, onClose: () -> Unit) {
    val s = items[index]
    val fmt = LocalFormatter.current
    val context = LocalContext.current
    val player = remember(session) { buildPlayer(context, session.api.http) }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(s.id) {
        val url = s.url?.let { session.mediaUrl(it) }
        if (s.type == "video" && url != null) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        } else {
            player.stop()
        }
    }
    DisposableEffect(player, index) {
        val l = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED && index < items.size - 1) onIndex(index + 1)
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val url = s.url?.let { session.mediaUrl(it) }
            when {
                url == null -> Text("This story was not saved.", color = Color.White, modifier = Modifier.align(Alignment.Center))
                s.type == "video" -> AndroidView(
                    factory = { PlayerView(it).apply { useController = true } },
                    update = { it.player = player },
                    onRelease = { it.player = null },
                    modifier = Modifier.fillMaxSize(),
                )
                else -> AsyncImage(url, null, imageLoader = session.imageLoader, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            // Tap zones for previous / next (images; videos keep their controls in the middle).
            Row(Modifier.fillMaxSize().padding(top = 64.dp, bottom = 96.dp)) {
                Box(Modifier.weight(1f).fillMaxSize().clickable(onClickLabel = "Previous story", enabled = index > 0) { onIndex(index - 1) })
                Box(Modifier.weight(if (s.type == "video") 2f else 0.01f).fillMaxSize())
                Box(Modifier.weight(1f).fillMaxSize().clickable(onClickLabel = "Next story", enabled = index < items.size - 1) { onIndex(index + 1) })
            }
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent))).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(s.username ?: s.userId, style = WaType.base.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                Text(fmt.listTime(s.takenAt), style = WaType.small, color = Color(0xDDFFFFFF), modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClickLabel = "Close", onClick = onClose), contentAlignment = Alignment.Center) {
                    WaIcon(WaIcons.close, 22.dp, Color.White, contentDescription = "Close")
                }
            }
        }
    }
}
