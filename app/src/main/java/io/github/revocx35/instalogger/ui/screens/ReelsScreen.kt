package io.github.revocx35.instalogger.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import io.github.revocx35.instalogger.core.ChatSummary
import io.github.revocx35.instalogger.core.WebComment
import io.github.revocx35.instalogger.core.WebPost
import io.github.revocx35.instalogger.core.userMessage
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
import io.github.revocx35.instalogger.ui.components.WaDialog
import io.github.revocx35.instalogger.ui.components.WaIcon
import io.github.revocx35.instalogger.ui.components.WaTextField
import io.github.revocx35.instalogger.ui.theme.Wa
import io.github.revocx35.instalogger.ui.theme.WaIcons
import io.github.revocx35.instalogger.ui.theme.WaType
import kotlinx.coroutines.launch

/** Instagram's reels feed through the server (`GET /api/reels`), with likes kept per reel while the screen lives. */
class ReelsViewModel(private val session: ServerSession) : ViewModel() {
    val reels = mutableStateListOf<WebPost>()
    var next by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var done by mutableStateOf(false)
        private set
    /** Reel id → (liked, like count) after the owner tapped the heart. */
    val likes = mutableStateMapOf<String, Pair<Boolean, Long?>>()
    /** Bumped on refresh so the pager jumps back to the top. */
    var generation by mutableStateOf(0)
        private set

    fun load(fresh: Boolean) {
        if (loading) return
        if (!fresh && (done || next == null) && reels.isNotEmpty()) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                val page = session.api.reels(if (fresh) null else next)
                if (fresh) {
                    reels.clear()
                    likes.clear()
                    generation++
                }
                val have = reels.map { it.id }.toHashSet()
                reels.addAll(page.items.filter { it.id !in have })
                next = page.next
                done = page.next == null
            } catch (e: Exception) {
                error = session.fail("reels", e)
            } finally {
                loading = false
            }
        }
    }

    fun liked(r: WebPost): Pair<Boolean, Long?> = likes[r.id] ?: (r.liked to r.likeCount)

    fun toggleLike(r: WebPost) {
        val (was, count) = liked(r)
        val on = !was
        likes[r.id] = on to count?.let { it + if (on) 1 else -1 }
        viewModelScope.launch {
            try {
                session.api.reelLike(r.id, on)
            } catch (e: Exception) {
                likes[r.id] = was to count
                error = session.fail("like", e)
            }
        }
    }
}

/** How many of the next reels get their video preloaded. */
private const val PRELOAD_AHEAD = 3

/** Asks the server for the first bytes of a video: it then fetches and caches the whole file in the background. */
private fun warm(session: ServerSession, url: String) {
    session.scope.launch(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            session.api.http.newCall(okhttp3.Request.Builder().url(url).header("Range", "bytes=0-1").build()).execute().close()
        }
    }
}

private fun compact(n: Long?): String = when {
    n == null -> ""
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0).replace(".0M", "M")
    n >= 10_000 -> "${n / 1000}K"
    n >= 1000 -> "%.1fK".format(n / 1000.0)
    else -> n.toString()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsScreen(vm: ReelsViewModel, session: ServerSession, connected: Boolean) {
    if (!connected) {
        EmptyState(WaIcons.reels, "Instagram is not connected", Modifier.fillMaxSize()) {
            MutedText("Reels play through the server's Instagram login. Log in on the Instagram screen first.")
        }
        return
    }
    LaunchedEffect(Unit) { if (vm.reels.isEmpty()) vm.load(fresh = true) }
    val context = LocalContext.current
    val player = remember(session) { buildPlayer(context, session.api.http).apply { repeatMode = Player.REPEAT_MODE_ONE } }
    var muted by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf<WebPost?>(null) }
    var commenting by remember { mutableStateOf<WebPost?>(null) }
    val pager = rememberPagerState { vm.reels.size + 1 }
    val warmed = remember(vm.generation) { HashSet<String>() }

    DisposableEffect(player) { onDispose { player.release() } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, player) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) player.pause() }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(vm.generation) { pager.scrollToPage(0) }
    // The current page plays; near the end the next page of the feed is fetched.
    LaunchedEffect(pager, vm.reels.size) {
        snapshotFlow { pager.currentPage }.collect { page ->
            val reel = vm.reels.getOrNull(page)
            val src = reel?.video?.takeIf { it.kind == "video" }?.src?.let { session.mediaUrl(it) }
            if (src != null) {
                player.setMediaItem(MediaItem.fromUri(src))
                player.prepare()
                player.playWhenReady = true
            } else {
                player.stop()
            }
            if (page >= vm.reels.size - 3) vm.load(fresh = false)
            // Warm the next reels' videos (the server fetches each one completely on its first request), so
            // swiping to them starts at once, like Instagram.
            for (r in vm.reels.drop(page + 1).take(PRELOAD_AHEAD)) {
                val url = r.video?.takeIf { it.kind == "video" }?.src?.let { session.mediaUrl(it) } ?: continue
                if (warmed.add(url)) warm(session, url)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { if (it < vm.reels.size) vm.reels[it].id else "end" }) { page ->
            val reel = vm.reels.getOrNull(page)
            if (reel == null) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (vm.loading) Spinner(32.dp)
                    ErrorNote(vm.error)
                    if (vm.error != null) WaButton("Try again", { vm.load(fresh = vm.reels.isEmpty()) })
                    if (vm.done && vm.reels.isNotEmpty()) MutedText("That's all for now.")
                }
            } else {
                ReelPage(reel, vm, session, player, active = page == pager.currentPage, onComments = { commenting = reel }, onShare = { sharing = reel })
            }
        }
        Row(Modifier.statusBarsPadding().padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RoundButton(if (vm.loading) null else WaIcons.refresh, "Refresh feed") { vm.load(fresh = true) }
            RoundButton(if (muted) WaIcons.mute else WaIcons.speaker, if (muted) "Sound on" else "Sound off") { muted = !muted }
        }
    }
    sharing?.let { ShareDialog(it, session) { sharing = null } }
    commenting?.let { CommentsDialog(it, session) { commenting = null } }
}

@Composable
private fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector?, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color(0x33FFFFFF)).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon == null) Spinner(18.dp) else WaIcon(icon, 20.dp, Color.White, contentDescription = label)
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun ReelPage(reel: WebPost, vm: ReelsViewModel, session: ServerSession, player: Player, active: Boolean, onComments: () -> Unit, onShare: () -> Unit) {
    val (liked, likes) = vm.liked(reel)
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        val poster = reel.video?.poster ?: reel.media.firstOrNull()?.src
        if (active && reel.video?.kind == "video") {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { it.player = player },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize(),
            )
        } else if (poster != null) {
            AsyncImage(session.mediaUrl(poster), null, imageLoader = session.imageLoader, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        // Tap: pause/play · double tap: like (like Instagram).
        Box(
            Modifier.fillMaxSize().pointerInput(reel.id) {
                detectTapGestures(
                    onTap = { if (active) player.playWhenReady = !player.playWhenReady },
                    onDoubleTap = { if (!vm.liked(reel).first) vm.toggleLike(reel) },
                )
            },
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xA6000000))))
                .padding(start = 14.dp, end = 76.dp, top = 48.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Avatar(reel.user.id, reel.user.username, reel.user.avatar, size = 32.dp)
                Text(reel.user.username, style = WaType.base.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                if (reel.user.verified) WaIcon(WaIcons.check, 14.dp, Color(0xFF0095F6), contentDescription = "Verified")
            }
            reel.caption?.let {
                Text(it, style = WaType.small, color = Color.White, maxLines = if (expanded) 12 else 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { expanded = !expanded })
            }
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Action(WaIcons.heart, compact(likes), if (liked) Color(0xFFED4956) else Color.White, if (liked) "Unlike" else "Like") { vm.toggleLike(reel) }
            Action(WaIcons.comment, compact(reel.commentCount), Color.White, "Comments", onClick = onComments)
            Action(WaIcons.send, "Send", Color.White, "Send to a chat", enabled = reel.code != null, onClick = onShare)
        }
    }
}

@Composable
private fun Action(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        Modifier.clip(CircleShape).clickable(enabled = enabled, onClickLabel = description, onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        WaIcon(icon, 28.dp, if (enabled) tint else tint.copy(alpha = 0.4f), contentDescription = description)
        if (label.isNotEmpty()) Text(label, style = WaType.tiny, color = Color.White)
    }
}

/** "Send to": posts the reel's link into the chosen chats (Instagram shows it as a reel card). */
@Composable
private fun ShareDialog(reel: WebPost, session: ServerSession, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var chats by remember { mutableStateOf<List<ChatSummary>?>(null) }
    var q by remember { mutableStateOf("") }
    val picked = remember { mutableStateListOf<String>() }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            chats = session.api.chats().filter { !it.removed }
        } catch (e: Exception) {
            error = e.userMessage()
        }
    }
    WaDialog("Send to", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WaTextField(q, { q = it }, null, placeholder = "Search")
            val list = (chats ?: emptyList()).filter { q.isBlank() || it.name.contains(q.trim(), true) || (it.username ?: "").contains(q.trim(), true) }.take(60)
            if (chats == null && error == null) Spinner()
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(list, key = { it.id }) { c ->
                    val on = c.id in picked
                    Row(
                        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .background(if (on) Wa.colors.active else Color.Transparent)
                            .clickable { if (on) picked.remove(c.id) else picked.add(c.id) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Avatar(c.id, c.name, c.avatarUrl, c.kind, size = 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = WaType.base, color = Wa.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            c.username?.takeIf { it != c.name }?.let { MutedText(it, style = WaType.small, maxLines = 1) }
                        }
                        if (on) WaIcon(WaIcons.check, 20.dp, Wa.colors.accent)
                    }
                }
            }
            WaTextField(note, { note = it }, null, placeholder = "Write a message…")
            ErrorNote(error)
            WaButton(if (picked.size > 1) "Send separately (${picked.size})" else "Send", {
                busy = true
                error = null
                scope.launch {
                    try {
                        val link = reel.link ?: return@launch
                        for (id in picked) {
                            session.api.sendText(id, link, null)
                            if (note.isNotBlank()) session.api.sendText(id, note.trim(), null)
                        }
                        onClose()
                    } catch (e: Exception) {
                        error = e.userMessage()
                    } finally {
                        busy = false
                    }
                }
            }, style = BtnStyle.Primary, enabled = picked.isNotEmpty(), busy = busy, block = true)
        }
    }
}

/** Comments with their first replies; post a comment or a reply ("@username …"). */
@Composable
private fun CommentsDialog(reel: WebPost, session: ServerSession, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val fmt = LocalFormatter.current
    val items = remember { mutableStateListOf<WebComment>() }
    var next by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var text by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<WebComment?>(null) }
    var posting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load(cursor: String?) {
        loading = true
        error = null
        try {
            val page = session.api.reelComments(reel.id, cursor)
            if (cursor == null) items.clear()
            items.addAll(page.items.filter { c -> items.none { it.id == c.id } })
            next = page.next
        } catch (e: Exception) {
            error = e.userMessage()
        } finally {
            loading = false
        }
    }
    LaunchedEffect(reel.id) { load(null) }

    @Composable
    fun line(c: WebComment, reply: Boolean) {
        Row(Modifier.padding(start = if (reply) 40.dp else 0.dp, top = 6.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(c.user.id, c.user.username, c.user.avatar, size = if (reply) 24.dp else 32.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold))
                        append(c.user.username)
                        pop()
                        append("  ")
                        append(c.text)
                    },
                    style = WaType.small,
                    color = Wa.colors.text,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MutedText(fmt.listTime(c.createdAt), style = WaType.tiny)
                    if (c.likeCount > 0) MutedText("${compact(c.likeCount)} likes", style = WaType.tiny)
                    Text("Reply", style = WaType.tiny.copy(fontWeight = FontWeight.SemiBold), color = Wa.colors.text2, modifier = Modifier.clickable {
                        replyTo = c
                        text = "@${c.user.username} "
                    })
                }
            }
        }
    }

    WaDialog("Comments", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyColumn(Modifier.heightIn(max = 380.dp).widthIn(min = 260.dp)) {
                items(items, key = { it.id }) { c ->
                    Column {
                        line(c, false)
                        for (r in c.replies) line(r, true)
                        if (c.replyCount > c.replies.size) MutedText("${c.replyCount - c.replies.size} more replies on Instagram", Modifier.padding(start = 40.dp), style = WaType.tiny)
                    }
                }
                item {
                    if (loading) Spinner()
                    if (!loading && items.isEmpty() && error == null) MutedText("No comments yet.", style = WaType.small)
                    if (!loading && next != null) WaButton("More comments", { scope.launch { load(next) } }, style = BtnStyle.Ghost)
                }
            }
            replyTo?.let { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MutedText("Replying to @${r.user.username}", Modifier.weight(1f), style = WaType.small)
                    Text("Cancel", style = WaType.small, color = Wa.colors.link, modifier = Modifier.clickable { replyTo = null; text = "" })
                }
            }
            WaTextField(text, { text = it }, null, placeholder = replyTo?.let { "Reply to @${it.user.username}…" } ?: "Add a comment…", singleLine = false)
            ErrorNote(error)
            WaButton("Post", {
                posting = true
                error = null
                scope.launch {
                    try {
                        val parent = replyTo
                        val mine = session.api.reelComment(reel.id, text.trim(), parent?.id).comment
                        if (mine != null) {
                            if (parent != null) {
                                val i = items.indexOfFirst { it.id == parent.id }
                                if (i >= 0) items[i] = items[i].copy(replies = items[i].replies + mine, replyCount = items[i].replyCount + 1)
                            } else {
                                items.add(0, mine)
                            }
                        }
                        text = ""
                        replyTo = null
                    } catch (e: Exception) {
                        error = e.userMessage()
                    } finally {
                        posting = false
                    }
                }
            }, style = BtnStyle.Primary, enabled = text.isNotBlank(), busy = posting, block = true)
        }
    }
}
