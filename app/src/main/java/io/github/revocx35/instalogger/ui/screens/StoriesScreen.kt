package io.github.revocx35.instalogger.ui.screens

import androidx.compose.foundation.background
import io.github.revocx35.instalogger.core.formatAgo
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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

private val RING = Brush.linearGradient(listOf(Color(0xFFFFD600), Color(0xFFFF7A00), Color(0xFFFF0069), Color(0xFFD300C5), Color(0xFF7638FA)), start = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY), end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f))

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
        GridCells.Adaptive(120.dp),
        Modifier.fillMaxSize().background(c.panel).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Stories", style = WaType.paneTitle, color = c.text, modifier = Modifier.weight(1f))
                    if (status?.enabled == true) {
                        WaButton("Check now", { vm.checkNow() }, icon = WaIcons.refresh, busy = vm.checking, enabled = connected)
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
                Column {
                    // Instagram's stories bar: gradient rings, the name under each.
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                        item { AuthorChip(null, "All", null, vm.user == null) { vm.select(null) } }
                        items(vm.authors, key = { it.id }) { a ->
                            AuthorChip(a, a.username ?: a.id, a.avatarUrl, vm.user == a.id) { vm.select(a.id) }
                        }
                    }
                    Box(Modifier.fillMaxWidth().padding(bottom = 2.dp).height(1.dp).background(c.border))
                }
            }
        }
        itemsIndexed(vm.items, key = { _, s -> s.id }) { i, s ->
            // A tile of Instagram's story archive: the day in a white box at the top left.
            Box(Modifier.aspectRatio(9f / 16f).background(Color(0xFF121212)).clickable { open = i }) {
                if (s.url != null && s.type == "image") {
                    AsyncImage(session.mediaUrl(s.url), null, imageLoader = session.imageLoader, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (s.url == null) {
                    Text(if (s.status == "too_large") "Too large" else if (s.status == "pending") "Downloading…" else "Not saved", style = WaType.small, color = Color(0xFFBBBBBB), modifier = Modifier.align(Alignment.Center))
                }
                val day = java.time.Instant.ofEpochMilli(s.takenAt).atZone(java.time.ZoneId.systemDefault())
                Column(
                    Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color.White).padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${day.dayOfMonth}", style = WaType.base.copy(fontSize = 15.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold), color = Color.Black)
                    Text(day.month.getDisplayName(java.time.format.TextStyle.SHORT, androidx.compose.ui.platform.LocalLocale.current.platformLocale).uppercase(), style = WaType.tiny.copy(fontSize = 9.sp, lineHeight = 10.sp), color = Color.Black)
                }
                if (s.type == "video") WaIcon(WaIcons.reels, 18.dp, Color.White, Modifier.align(Alignment.TopEnd).padding(8.dp))
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xA6000000)))).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.username ?: s.userId, style = WaType.tiny.copy(fontWeight = FontWeight.SemiBold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(formatAgo(s.takenAt), style = WaType.tiny, color = Color(0xCCFFFFFF))
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
                if (!vm.loading && vm.next != null) WaButton("Older stories", { vm.more() }, Modifier.padding(top = 12.dp))
            }
        }
    }
    open?.let { i ->
        if (i in vm.items.indices) StoryViewer(vm.items, i, session, { id -> vm.authors.firstOrNull { it.id == id }?.avatarUrl }, { open = it }) { open = null }
    }
}

@Composable
private fun AuthorChip(a: StoryAuthor?, label: String, avatar: String?, selected: Boolean, onClick: () -> Unit) {
    val c = Wa.colors
    Column(Modifier.width(72.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box {
            val ring = if (a == null && !selected) Brush.linearGradient(listOf(c.border, c.border)) else RING
            Box(Modifier.size(66.dp).clip(CircleShape).background(ring).padding(2.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(c.panel).padding(2.dp), contentAlignment = Alignment.Center) {
                    if (a == null) {
                        Box(Modifier.fillMaxSize().clip(CircleShape).background(c.panel2), contentAlignment = Alignment.Center) { WaIcon(WaIcons.stories, 28.dp, c.text) }
                    } else {
                        Avatar(a.id, label, avatar, size = 58.dp)
                    }
                }
            }
            if (a != null) {
                Text(
                    "${a.count}",
                    style = WaType.tiny.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(10.dp)).background(c.panel).padding(2.dp)
                        .clip(RoundedCornerShape(8.dp)).background(c.like).padding(horizontal = 5.dp),
                )
            }
        }
        Text(label, style = WaType.tiny.copy(fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** How long a photo story stays before moving on (Instagram uses about 5 s). */
private const val PHOTO_MS = 5000L

/**
 * Instagram's story viewer: progress bars for this account's run of stories, avatar + name + age, tap the left or
 * right side to go back or on, tap the middle to pause; photos move on after 5 s, videos when they end.
 */
@Composable
private fun StoryViewer(items: List<StoryItem>, index: Int, session: ServerSession, avatarOf: (String) -> String?, onIndex: (Int) -> Unit, onClose: () -> Unit) {
    val s = items[index]
    val context = LocalContext.current
    val player = remember(session) { buildPlayer(context, session.api.http) }
    var paused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    DisposableEffect(player) { onDispose { player.release() } }
    val next = { if (index < items.size - 1) onIndex(index + 1) else onClose() }
    LaunchedEffect(s.id) {
        progress = 0f
        val url = s.url?.let { session.mediaUrl(it) }
        if (s.type == "video" && url != null) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = !paused
        } else {
            player.stop()
        }
    }
    LaunchedEffect(paused) { player.playWhenReady = !paused }
    // Progress: a timer for photos, the player's position for videos.
    LaunchedEffect(s.id, paused) {
        if (paused) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(50)
            if (s.type == "video") {
                val d = player.duration
                if (d > 0) progress = (player.currentPosition.toFloat() / d).coerceIn(0f, 1f)
            } else if (s.url != null) {
                progress += 50f / PHOTO_MS
                if (progress >= 1f) {
                    next()
                    break
                }
            }
        }
    }
    DisposableEffect(player, index) {
        val l = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) next()
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }
    var first = index
    while (first > 0 && items[first - 1].userId == s.userId) first--
    var last = index
    while (last < items.size - 1 && items[last + 1].userId == s.userId) last++
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val url = s.url?.let { session.mediaUrl(it) }
            when {
                url == null -> Text("This story was not saved.", color = Color.White, modifier = Modifier.align(Alignment.Center))
                s.type == "video" -> AndroidView(
                    factory = { PlayerView(it).apply { useController = false } },
                    update = { it.player = player },
                    onRelease = { it.player = null },
                    modifier = Modifier.fillMaxSize(),
                )
                else -> AsyncImage(url, null, imageLoader = session.imageLoader, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            // Tap zones: previous / pause / next.
            Row(Modifier.fillMaxSize().padding(top = 72.dp)) {
                Box(Modifier.weight(1f).fillMaxSize().clickable(onClickLabel = "Previous story", indication = null, interactionSource = null) { if (index > 0) onIndex(index - 1) })
                Box(Modifier.weight(1f).fillMaxSize().clickable(onClickLabel = if (paused) "Play" else "Pause", indication = null, interactionSource = null) { paused = !paused })
                Box(Modifier.weight(1f).fillMaxSize().clickable(onClickLabel = "Next story", indication = null, interactionSource = null) { next() })
            }
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0x73000000), Color.Transparent))).statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (k in first..last) {
                        val f = if (k < index) 1f else if (k > index) 0f else progress
                        Box(Modifier.weight(1f).height(2.dp).clip(RoundedCornerShape(1.dp)).background(Color(0x59FFFFFF))) {
                            Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(Color.White))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Avatar(s.userId, s.username ?: s.userId, avatarOf(s.userId), size = 32.dp)
                    Text(s.username ?: s.userId, style = WaType.base.copy(fontWeight = FontWeight.SemiBold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatAgo(s.takenAt), style = WaType.base, color = Color(0xBFFFFFFF), modifier = Modifier.weight(1f))
                    Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = if (paused) "Play" else "Pause") { paused = !paused }, contentAlignment = Alignment.Center) {
                        WaIcon(if (paused) WaIcons.play else WaIcons.pause, 20.dp, Color.White, contentDescription = if (paused) "Play" else "Pause")
                    }
                    Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = "Close", onClick = onClose), contentAlignment = Alignment.Center) {
                        WaIcon(WaIcons.close, 26.dp, Color.White, contentDescription = "Close")
                    }
                }
            }
        }
    }
}
