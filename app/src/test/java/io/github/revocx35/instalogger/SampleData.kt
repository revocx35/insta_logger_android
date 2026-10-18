package io.github.revocx35.instalogger

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Base64
import io.github.revocx35.instalogger.core.ChatKind
import io.github.revocx35.instalogger.core.ChatRef
import io.github.revocx35.instalogger.core.ChatSummary
import io.github.revocx35.instalogger.core.DeletedFeedItem
import io.github.revocx35.instalogger.core.MediaInfo
import io.github.revocx35.instalogger.core.MediaStatus
import io.github.revocx35.instalogger.core.Mention
import io.github.revocx35.instalogger.core.Message
import io.github.revocx35.instalogger.core.MessagePreview
import io.github.revocx35.instalogger.core.MessageType
import io.github.revocx35.instalogger.core.QuotedSnapshot
import io.github.revocx35.instalogger.core.Reaction
import io.github.revocx35.instalogger.core.SearchHit
import io.github.revocx35.instalogger.core.MusicInfo
import io.github.revocx35.instalogger.core.ShareInfo
import io.github.revocx35.instalogger.core.IgMe
import io.github.revocx35.instalogger.core.IgState
import io.github.revocx35.instalogger.core.IgStatus
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneId

/** Instagram-like conversations (like insta_logger's `server/src/testutil/seed.ts`), as API DTOs. */
object SampleData {
    const val ALI = "340282366841710300949128100000001"
    const val AYSE = "340282366841710300949128100000002"
    const val CAN = "340282366841710300949128100000003"
    const val GROUP = "340282366841710300949128100000009"
    private const val ALI_U = "17841400000000001"
    private const val AYSE_U = "17841400000000002"
    private const val CAN_U = "17841400000000003"

    val zone: ZoneId = ZoneId.of("Europe/Istanbul")
    val now: Long = LocalDateTime.of(2026, 10, 18, 15, 0).atZone(zone).toInstant().toEpochMilli()
    private const val H = 3_600_000L
    private const val D = 24 * H

    private fun checker(w: Int, h: Int, color: Int): String {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint()
        for (y in 0 until h step 16) for (x in 0 until w step 16) {
            p.color = if (((x / 16) + (y / 16)) % 2 == 0) color else (color and 0x00FFFFFF) or (0xCC shl 24)
            c.drawRect(x.toFloat(), y.toFloat(), (x + 16).toFloat(), (y + 16).toFloat(), p)
        }
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private var n = 0
    private fun m(chat: String, from: String?, ts: Long, type: MessageType = MessageType.Text, text: String? = null, build: Message.() -> Message = { this }): Message {
        val fromMe = from == null
        val names = mapOf(ALI_U to "Ali Veli", AYSE_U to "Ayşe Kaya", CAN_U to "Can Demir")
        val users = mapOf(ALI_U to "ali.veli", AYSE_U to "ayse_k", CAN_U to "can.demir")
        return Message(
            id = "mid.\$${++n}", chatId = chat, senderId = from ?: "17841400000000099", senderName = from?.let { names[it] }, senderUsername = from?.let { users[it] },
            fromMe = fromMe, ts = ts, type = type, text = text, ack = if (fromMe) 3 else null, capturedAt = ts,
        ).build()
    }

    val aliChat: List<Message> by lazy {
        val t0 = now - 3 * D - 3 * H
        val q = m(ALI, null, t0 + 60_000, text = "Yes! Looking forward to it — plan: https://example.com/plan_v2")
        listOf(
            m(ALI, ALI_U, t0, text = "Hey! Are we still on for Saturday? 🎉"),
            q,
            m(ALI, ALI_U, t0 + 120_000, text = "Perfect 👍") { copy(quoted = QuotedSnapshot(q.id, null, true, MessageType.Text, q.text)) },
            m(ALI, ALI_U, t0 + 180_000, text = "Actually I told Ayşe your secret 🙈 (oops)") { copy(deletedAt = t0 + 180_000, deletedBy = "sender") },
            m(ALI, ALI_U, now - D, text = "Meet at 8pm instead") { copy(edited = true, editCount = 1, editedAt = now - D) },
            m(ALI, ALI_U, now - D + 400_000, MessageType.Image) {
                copy(media = MediaInfo(MediaStatus.Downloaded, "/api/media/1", "image/png", null, 40_000, 640.0, 400.0, null, checker(160, 100, 0xFF3797F0.toInt())))
            },
            m(ALI, ALI_U, now - D + 1_400_000, MessageType.Share) {
                copy(share = ShareInfo("Reel", "natgeo", "National Geographic", "Whales off the coast of Norway 🐋", "https://www.instagram.com/reel/DehZj9wggt0/"))
            },
            m(ALI, null, now - D + 2_400_000, MessageType.Voice) { copy(media = MediaInfo(MediaStatus.Failed, null, "audio/mp4", null, 24_000, null, null, 12.0)) },
            m(ALI, ALI_U, now - H, MessageType.ViewOnce) { copy(media = MediaInfo(MediaStatus.ViewOnce)) },
            m(ALI, null, now - H / 2, text = "See you there @ali.veli 😄") {
                copy(reactions = listOf(Reaction(ALI_U, "Ali Veli", "❤️", now - H / 2)))
            },
        )
    }

    val groupChat: List<Message> by lazy {
        val g0 = now - 2 * D - 2 * H
        listOf(
            m(GROUP, CAN_U, g0 - 1_000_000, MessageType.System, "Can Demir named the group Weekend plans"),
            m(GROUP, AYSE_U, g0, text = "Who is bringing dessert? 🍰"),
            m(GROUP, CAN_U, g0 + 30_000, text = "Me! @ayse_k you bring drinks") { copy(mentions = listOf(Mention(AYSE_U, "Ayşe Kaya"))) },
            m(GROUP, CAN_U, g0 + 60_000, MessageType.Music) { copy(music = MusicInfo("Sunflower", "Post Malone")) },
            m(GROUP, CAN_U, g0 + 150_000, MessageType.Image) {
                copy(forwarded = true, deletedAt = g0 + 150_000, deletedBy = "missing", media = MediaInfo(MediaStatus.Downloaded, "/api/media/3", "image/png", null, 30_000, 400.0, 400.0, null, checker(100, 100, 0xFFED4956.toInt())))
            },
            m(GROUP, null, g0 + 200_000, text = "See you all 👋"),
        )
    }

    val chats: List<ChatSummary> by lazy {
        listOf(
            ChatSummary(GROUP, ChatKind.Group, "Weekend plans", null, null, now - 2 * D, MessagePreview("x", MessageType.Image, false, "Can Demir", "Photo", true, now - 2 * D), pinned = true, messageCount = 6, deletedCount = 1),
            ChatSummary(AYSE, ChatKind.User, "Ayşe Kaya", "ayse_k", null, now - 60_000, MessagePreview("y", MessageType.Sticker, false, "Ayşe Kaya", "Sticker", false, now - 60_000), messageCount = 121),
            ChatSummary(ALI, ChatKind.User, "Ali Veli", "ali.veli", null, now - H / 2, MessagePreview("z", MessageType.Text, true, null, "See you there @ali.veli 😄", false, now - H / 2), messageCount = 10, deletedCount = 1),
            ChatSummary("340282366841710300949128100000004", ChatKind.User, "shop.istanbul", "shop.istanbul", null, now - 2 * H, MessagePreview("s", MessageType.Text, false, "shop.istanbul", "Your order has shipped", false, now - 2 * H), folder = "pending", messageCount = 1),
            ChatSummary("340282366841710300949128100000005", ChatKind.User, "old.friend", "old.friend", null, now - 9 * D, MessagePreview("o", MessageType.Text, false, null, "Long time no see!", false, now - 9 * D), removed = true, messageCount = 3),
        )
    }

    val deleted: List<DeletedFeedItem> by lazy {
        listOf(
            DeletedFeedItem(ChatRef(GROUP, "Weekend plans", ChatKind.Group), groupChat[4]),
            DeletedFeedItem(ChatRef(ALI, "Ali Veli", ChatKind.User), aliChat[3]),
        )
    }

    val hits: List<SearchHit> by lazy {
        listOf(SearchHit(ChatRef(ALI, "Ali Veli", ChatKind.User), aliChat[1]), SearchHit(ChatRef(ALI, "Ali Veli", ChatKind.User), aliChat[3]))
    }

    val ready = IgStatus(IgState.Ready, "Connected", IgMe("17841400000000099", "owner.account", "Owner Person"), null, 0, null, now)
}
