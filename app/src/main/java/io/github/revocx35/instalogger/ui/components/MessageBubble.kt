package io.github.revocx35.instalogger.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.revocx35.instalogger.core.Message
import io.github.revocx35.instalogger.core.MessageEdit
import io.github.revocx35.instalogger.core.MessageType
import io.github.revocx35.instalogger.core.IgText
import io.github.revocx35.instalogger.core.colorFor
import io.github.revocx35.instalogger.core.formatDuration
import io.github.revocx35.instalogger.core.userMessage
import io.github.revocx35.instalogger.ui.LocalFormatter
import io.github.revocx35.instalogger.ui.LocalMessageActions
import io.github.revocx35.instalogger.ui.LocalSelection
import io.github.revocx35.instalogger.ui.MessageSelection
import io.github.revocx35.instalogger.ui.theme.Wa
import io.github.revocx35.instalogger.ui.theme.WaIcons
import io.github.revocx35.instalogger.ui.theme.WaType

private val QUOTE_LABEL = mapOf(
    MessageType.Image to "📷 Photo",
    MessageType.Video to "🎥 Video",
    MessageType.Gif to "GIF",
    MessageType.Voice to "🎤 Voice message",
    MessageType.Sticker to "Sticker",
    MessageType.Share to "↗ Shared post",
    MessageType.Music to "🎵 Music",
)

@Composable
private fun Ticks(ack: Int?, onBlue: Boolean = false) {
    val c = Wa.colors
    val plain = if (onBlue) c.bubbleOutText.copy(alpha = 0.75f) else c.muted
    val read = if (onBlue) c.bubbleOutText else c.tickRead
    when {
        ack == null || ack < 0 -> Unit
        ack == 0 -> WaIcon(WaIcons.clock, 14.dp, plain, contentDescription = "Pending")
        ack == 1 -> WaIcon(WaIcons.check, 16.dp, plain, contentDescription = "Sent")
        else -> WaIcon(WaIcons.checks, 16.dp, if (ack >= 3) read else plain, contentDescription = if (ack >= 3) "Seen" else "Delivered")
    }
}

/** Centered pill for system messages and call logs. */
@Composable
fun SystemPill(text: String, modifier: Modifier = Modifier) {
    val c = Wa.colors
    Box(modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = WaType.small.copy(fontSize = 12.sp),
            color = c.text2,
            textAlign = TextAlign.Center,
            modifier = Modifier.shadow(1.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).background(c.system).padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun EditHistoryDialog(msg: Message, onClose: () -> Unit) {
    val actions = LocalMessageActions.current
    val fmt = LocalFormatter.current
    val c = Wa.colors
    var edits by remember { mutableStateOf<List<MessageEdit>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg.id) {
        try {
            edits = actions.edits(msg.id)
        } catch (e: Exception) {
            error = e.userMessage()
        }
    }
    WaDialog("Edit history", onClose) {
        ErrorNote(error)
        if (edits == null && error == null) Spinner()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val all = (edits ?: emptyList()).map { "Earlier version · captured ${fmt.dateTime(it.capturedAt)}" to it.body } +
                listOf(("Current version" + (msg.editedAt?.let { " · edited ${fmt.dateTime(it)}" } ?: "")) to msg.text)
            all.forEachIndexed { i, (label, body) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MutedText("${i + 1}. $label", style = WaType.small)
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(c.panel2).padding(8.dp)) {
                        if (body != null) WaTextView(body) else Text("(no text)", color = c.muted, fontStyle = FontStyle.Italic)
                    }
                }
            }
        }
        if (edits?.isEmpty() == true) {
            MutedText("The previous text was not captured (the edit happened before this message was logged).", Modifier.padding(top = 12.dp), style = WaType.small)
        }
    }
}

@Composable
private fun ReactionsDialog(msg: Message, onClose: () -> Unit) {
    WaDialog("Reactions", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in msg.reactions) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(r.emoji, style = WaType.base.copy(fontSize = 22.sp))
                    Text(r.senderName ?: r.senderId, style = WaType.base)
                }
            }
        }
    }
}

/** A shared post / reel / story / profile / link, and music stickers (Instagram's "XMA" attachments). */
@Composable
private fun Special(msg: Message) {
    val c = Wa.colors
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val share = msg.share
    if (share != null) {
        val url = share.url?.let { IgText.safeHref(it) }
        Column(
            Modifier.padding(bottom = 4.dp).widthIn(min = 200.dp).clip(RoundedCornerShape(10.dp)).background(c.quoteBg)
                .then(if (url != null) Modifier.clickable(onClickLabel = "Open link") { runCatching { uri.openUri(url) } } else Modifier)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            share.eyebrow?.let { MutedText(it, style = WaType.tiny) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WaIcon(WaIcons.forward, 16.dp, c.text2)
                Text(listOfNotNull(share.kind, share.title).joinToString(" · ").ifEmpty { "Shared" }, style = WaType.bubble.copy(fontWeight = FontWeight.SemiBold), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            share.subtitle?.let { MutedText(it, style = WaType.small, maxLines = 2) }
            share.caption?.let { Text(it, style = WaType.small, color = c.text, maxLines = 4, overflow = TextOverflow.Ellipsis) }
            if (url != null) Text(url.removePrefix("https://").removePrefix("www.").take(60), style = WaType.tiny, color = c.link, maxLines = 1)
        }
    }
    val music = msg.music
    if (music != null) {
        Row(Modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🎵", style = WaType.bubble)
            Text(listOfNotNull(music.title, music.artist).joinToString(" — ").ifEmpty { "Music" }, style = WaType.bubble, color = c.text)
        }
    }
}

/**
 * Lays out bubble content with the time/ticks row at its bottom-right corner: text flows around it
 * (via [WaTextView]'s trailing space); other content ends with a line for it.
 */
@Composable
private fun WithMeta(meta: @Composable () -> Unit, content: @Composable (metaWidth: Dp) -> Unit) {
    val density = LocalDensity.current
    SubcomposeLayout { constraints ->
        val metaPlaceable = subcompose("meta", meta).map { it.measure(Constraints()) }
        val mw = metaPlaceable.maxOfOrNull { it.width } ?: 0
        val mh = metaPlaceable.maxOfOrNull { it.height } ?: 0
        val contentPlaceable = subcompose("content") { content(with(density) { mw.toDp() }) }.map { it.measure(constraints.copy(minWidth = 0)) }
        val cw = maxOf(contentPlaceable.maxOfOrNull { it.width } ?: 0, mw).coerceAtMost(constraints.maxWidth)
        val ch = contentPlaceable.sumOf { it.height }
        val h = maxOf(ch, mh)
        layout(cw, h) {
            var y = 0
            contentPlaceable.forEach { it.place(0, y); y += it.height }
            metaPlaceable.forEach { it.place(cw - mw, h - mh) }
        }
    }
}

/**
 * One logged message, like the web's MessageBubble: unsent/edited/forwarded/quoted states, sender
 * names in groups, media, shared posts, music, reactions and the seen tick.
 */
@OptIn(ExperimentalFoundationApi::class)
/** The web's `--out-grad` (top of the screen → bottom). */
private val OUT_GRADIENT = listOf(Color(0xFFA033FF), Color(0xFF6B4DFC), Color(0xFF3797F0))

@Composable
fun MessageBubble(
    msg: Message,
    isGroup: Boolean,
    showSender: Boolean,
    highlighted: Boolean,
    maxBubbleWidth: Dp,
    modifier: Modifier = Modifier,
    /** The message before / after is from the same side: the joined corners get small, like Instagram's runs. */
    joinTop: Boolean = false,
    joinBottom: Boolean = false,
) {
    val c = Wa.colors
    val fmt = LocalFormatter.current
    val actions = LocalMessageActions.current

    if (msg.type == MessageType.System) {
        SystemPill("${msg.text ?: "System message"} · ${fmt.time(msg.ts)}", modifier)
        return
    }

    var showEdits by remember { mutableStateOf(false) }
    var showReactions by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val deleted = msg.deletedAt != null
    val neverCaptured = false
    val sticker = msg.type == MessageType.Sticker && msg.media?.url != null
    // Photos and videos without text sit on their own, without a bubble (like Instagram).
    val mediaOnly = (msg.type == MessageType.Image || msg.type == MessageType.Video) && msg.media?.url != null && msg.text == null && msg.quoted == null
    val big = 22.dp
    val small = 4.dp
    val shape = if (msg.fromMe) {
        RoundedCornerShape(topStart = big, topEnd = if (joinTop) small else big, bottomEnd = if (joinBottom) small else big, bottomStart = big)
    } else {
        RoundedCornerShape(topStart = if (joinTop) small else big, topEnd = big, bottomEnd = big, bottomStart = if (joinBottom) small else big)
    }
    val bubbleBg = when {
        sticker || mediaOnly -> Color.Transparent
        msg.fromMe -> c.bubbleOut
        else -> c.bubbleIn
    }
    // Own bubbles share one screen-tall purple→blue gradient (Instagram's): each bubble paints the slice at its
    // position. The position is read in the draw phase only, so scrolling redraws without recomposing.
    val outGradient = msg.fromMe && !sticker && !mediaOnly
    val windowY = remember { mutableFloatStateOf(0f) }
    val screenH = LocalWindowInfo.current.containerSize.height.toFloat()
    val innerMax = maxBubbleWidth - 17.dp
    // Own messages: white text on Instagram's blue bubble.
    val onBlue = msg.fromMe && !sticker && !mediaOnly
    val fg = if (onBlue) c.bubbleOutText else c.text
    val metaColor = if (onBlue) c.bubbleOutText.copy(alpha = 0.75f) else c.muted
    val selection = LocalSelection.current
    val selectable = selection != null && MessageSelection.selectable(msg)
    val selected = selection?.isSelected(msg.id) == true

    Column(
        modifier.fillMaxWidth()
            .then(if (selected) Modifier.background(c.accent.copy(alpha = 0.16f)) else Modifier)
            .then(if (selection?.active == true && selectable) Modifier.clickable(onClickLabel = if (selected) "Unselect" else "Select") { selection.toggle(msg) } else Modifier)
            .padding(vertical = 1.dp),
        horizontalAlignment = if (msg.fromMe) Alignment.End else Alignment.Start,
    ) {
        Box(
            Modifier
                .widthIn(max = maxBubbleWidth)
                .then(if (sticker) Modifier else Modifier.shadow(1.dp, shape, ambientColor = c.shadow, spotColor = c.shadow))
                .then(
                    when {
                        selected -> Modifier.border(3.dp, c.accent, shape)
                        highlighted -> Modifier.border(3.dp, c.warn, shape)
                        deleted -> Modifier.border(2.dp, c.danger, shape)
                        else -> Modifier
                    },
                )
                .clip(shape)
                .then(
                    if (outGradient) {
                        Modifier
                            .onGloballyPositioned { windowY.floatValue = it.positionInWindow().y }
                            .drawBehind { drawRect(Brush.verticalGradient(OUT_GRADIENT, startY = -windowY.floatValue, endY = screenH - windowY.floatValue)) }
                    } else {
                        Modifier.background(bubbleBg)
                    },
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (selection?.active == true && selectable) selection.toggle(msg) },
                    onLongClickLabel = "Message options",
                    onLongClick = {
                        // While selecting photos/videos a long-press toggles; otherwise it opens the message menu
                        // (reply, react, copy, edit, delete for everyone, select).
                        if (selection?.active == true && selectable) selection.toggle(msg) else actions.longPress(msg)
                    },
                )
                .padding(if (mediaOnly) PaddingValues(0.dp) else PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)),
        ) {
            WithMeta(
                meta = {
                    // Under a bare photo/video the time keeps clear of the rounded corner.
                    Row(Modifier.padding(start = 10.dp, top = 4.dp, end = if (mediaOnly) 12.dp else 0.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (msg.edited) {
                            LinkButton(
                                "Edited" + if (msg.editCount > 1) " ×${msg.editCount}" else "",
                                { showEdits = true },
                                style = WaType.meta,
                                color = metaColor,
                                icon = WaIcons.edit,
                            )
                        }
                        Text(fmt.time(msg.ts), style = WaType.meta, color = metaColor)
                        if (msg.fromMe) Ticks(msg.ack, onBlue)
                    }
                },
            ) { metaWidth ->
                val trailing = with(LocalDensity.current) { (metaWidth + 4.dp).toSp() }
                Column {
                    if (deleted) {
                        val at = msg.deletedAt!!
                        val sameDay = fmt.dayKey(at) == fmt.dayKey(msg.ts)
                        Row(Modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            WaIcon(WaIcons.ban, 14.dp, c.danger)
                            Text(
                                (if (msg.deletedBy == "missing") "Gone from Instagram" else "Unsent") + " · " +
                                    (if (sameDay) fmt.time(at) else fmt.dateTime(at)) + if (neverCaptured) "" else " · logged copy",
                                style = WaType.tiny.copy(fontSize = 11.7.sp, fontWeight = FontWeight.SemiBold),
                                color = c.danger,
                            )
                        }
                    }
                    if (isGroup && !msg.fromMe && showSender) {
                        Text(
                            msg.senderName ?: "Unknown",
                            style = WaType.small.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(colorFor(msg.senderId ?: msg.senderName ?: "")),
                            modifier = Modifier.padding(bottom = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (msg.expiresAt != null) MutedText("Disappearing message", style = WaType.small)
                    if (msg.forwarded) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            WaIcon(WaIcons.forward, 14.dp, c.muted)
                            Text("Forwarded", style = WaType.tiny.copy(fontSize = 11.7.sp, fontStyle = FontStyle.Italic), color = c.muted)
                        }
                    }
                    val quoted = msg.quoted
                    if (quoted != null) {
                        Row(
                            Modifier
                                .padding(top = 2.dp, bottom = 5.dp)
                                .widthIn(max = innerMax)
                                .height(IntrinsicSize.Min)
                                .clip(RoundedCornerShape(6.dp))
                                .background(c.quoteBg)
                                .clickable(enabled = quoted.id != null, onClickLabel = "Jump to quoted message") { quoted.id?.let(actions::jump) },
                        ) {
                            Box(Modifier.width(4.dp).fillMaxHeight().background(c.accent))
                            Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                Text(
                                    if (quoted.fromMe) "You" else quoted.senderName ?: "Unknown",
                                    style = WaType.tiny.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                    color = c.accentOnPanel,
                                    maxLines = 1,
                                )
                                Text(
                                    quoted.text?.let { IgText.strip(it) } ?: QUOTE_LABEL[quoted.type] ?: "Message",
                                    style = WaType.small,
                                    color = c.text2,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    var endsWithText = false
                    when {
                        neverCaptured -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            WaIcon(WaIcons.ban, 14.dp, c.muted)
                            Text("This message was unsent before it could be logged.", style = WaType.bubble.copy(fontStyle = FontStyle.Italic), color = c.muted)
                        }
                        msg.type == MessageType.ViewOnce -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            WaIcon(WaIcons.eye, 14.dp, c.muted)
                            Text("View-once photo or video (never logged)", style = WaType.bubble.copy(fontStyle = FontStyle.Italic), color = c.muted)
                        }
                        else -> {
                            MessageMedia(msg, innerMax)
                            Special(msg)
                            val text = msg.text
                            if (text != null) {
                                endsWithText = remember(text) { endsWithInlineText(text) }
                                WaTextView(text, color = fg, trailing = if (endsWithText) trailing else 0.sp)
                            }
                            if (msg.type == MessageType.Unknown && text == null) {
                                Text("Unsupported message type", style = WaType.small.copy(fontStyle = FontStyle.Italic), color = c.muted)
                            }
                        }
                    }
                    // Room for the time row when it can't flow into the last line of text.
                    if (!endsWithText) Spacer(Modifier.height(16.dp))
                }
            }
        }
        if (msg.reactions.isNotEmpty()) {
            val emojis = msg.reactions.map { it.emoji }.distinct().take(4).joinToString("")
            Text(
                emojis + if (msg.reactions.size > 1) " ${msg.reactions.size}" else "",
                style = WaType.small,
                color = c.text,
                modifier = Modifier
                    .offset(y = (-4).dp)
                    .padding(horizontal = 8.dp)
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.panel)
                    .clickable(onClickLabel = "Show reactions") { showReactions = true }
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
    if (showEdits) EditHistoryDialog(msg) { showEdits = false }
    if (showReactions) ReactionsDialog(msg) { showReactions = false }
}
