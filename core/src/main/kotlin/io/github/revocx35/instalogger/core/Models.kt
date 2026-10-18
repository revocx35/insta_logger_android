package io.github.revocx35.instalogger.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * Kotlin mirror of the server↔client contract in insta_logger's `shared/api.d.ts`. Keep both in sync.
 * Timestamps are Unix epoch milliseconds. Unknown enum values fall back to a default
 * (see [ApiJson]) so a newer server never crashes an older app.
 */

val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    encodeDefaults = false
}

@Serializable
data class ApiErrorBody(val error: ApiErrorDetail? = null)

@Serializable
data class ApiErrorDetail(val code: String = "http_error", val message: String = "", val retryAfter: Int? = null)

/* ------------------------------------------------------------------ state */

@Serializable
enum class IgState {
    @SerialName("idle") Idle,
    @SerialName("starting") Starting,
    @SerialName("login") Login,
    @SerialName("challenge") Challenge,
    @SerialName("syncing") Syncing,
    @SerialName("ready") Ready,
    @SerialName("error") Error,
}

@Serializable
data class IgMe(val id: String, val username: String? = null, val name: String? = null)

@Serializable
data class SyncProgress(val phase: String = "history", val chatsDone: Int = 0, val chatsTotal: Int = 0)

@Serializable
data class IgStatus(
    val state: IgState = IgState.Idle,
    val detail: String? = null,
    val me: IgMe? = null,
    val sync: SyncProgress? = null,
    val mediaQueue: Int = 0,
    val lastEventAt: Long? = null,
    val since: Long = 0,
) {
    val connected: Boolean get() = state == IgState.Ready || state == IgState.Syncing
    val needsLogin: Boolean get() = state == IgState.Login || state == IgState.Challenge
}

@Serializable
data class AppState(
    val hasOwner: Boolean = false,
    val authenticated: Boolean = false,
    val onboardingComplete: Boolean = false,
    val csrfToken: String? = null,
    val username: String? = null,
    val totpEnabled: Boolean? = null,
    val ig: IgStatus? = null,
)

/* -------------------------------------------------------------------- auth */

@Serializable data class SignupRequest(val setupToken: String, val username: String, val password: String)
@Serializable data class SignupResponse(val recoveryKey: String)
@Serializable data class LoginRequest(val username: String, val password: String, val totp: String? = null)
@Serializable data class LoginResponse(val ok: Boolean = false, val needTotp: Boolean? = null)
@Serializable data class RecoverRequest(val username: String, val recoveryKey: String, val newPassword: String)
@Serializable data class RecoverResponse(val recoveryKey: String)
@Serializable data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class SessionInfo(
    val id: String,
    val current: Boolean = false,
    val createdAt: Long = 0,
    val lastSeenAt: Long = 0,
    val expiresAt: Long = 0,
    val ip: String? = null,
    val userAgent: String? = null,
)

@Serializable data class TotpSetupResponse(val secret: String, val otpauthUrl: String)
@Serializable data class TotpEnableRequest(val code: String, val password: String)
@Serializable data class TotpDisableRequest(val password: String, val code: String)
@Serializable data class RotateRecoveryKeyRequest(val password: String, val totp: String? = null)
@Serializable data class RotateRecoveryKeyResponse(val recoveryKey: String)
@Serializable data class VncCredentials(val password: String)
@Serializable data class OkResponse(val ok: Boolean = true)
@Serializable data class ForceRequest(val force: Boolean? = null)
/** `POST /api/ig/session`: log in with a session (cookies) copied from the owner's own browser. */
@Serializable data class ImportSessionRequest(val cookies: String)

/* -------------------------------------------------------------- chats etc. */

@Serializable
enum class ChatKind {
    @SerialName("user") User,
    @SerialName("group") Group,
}

@Serializable
data class ChatSummary(
    val id: String,
    val kind: ChatKind = ChatKind.User,
    val name: String = "",
    /** The other person's Instagram username in one-to-one chats. */
    val username: String? = null,
    val avatarUrl: String? = null,
    val lastTs: Long? = null,
    val lastMessage: MessagePreview? = null,
    /** Message requests and other non-primary folders. */
    val folder: String? = null,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val removed: Boolean = false,
    val messageCount: Int = 0,
    val deletedCount: Int = 0,
)

@Serializable
data class MessagePreview(
    val id: String,
    val type: MessageType = MessageType.Unknown,
    val fromMe: Boolean = false,
    val senderName: String? = null,
    val text: String? = null,
    val deleted: Boolean = false,
    val ts: Long = 0,
)

/** text · image · video · gif · voice · sticker · share (post/reel/story/profile/link) · music · system · view_once · unknown */
@Serializable
enum class MessageType {
    @SerialName("text") Text,
    @SerialName("image") Image,
    @SerialName("video") Video,
    @SerialName("gif") Gif,
    @SerialName("voice") Voice,
    @SerialName("sticker") Sticker,
    @SerialName("share") Share,
    @SerialName("music") Music,
    @SerialName("system") System,
    @SerialName("view_once") ViewOnce,
    @SerialName("unknown") Unknown,
}

@Serializable
enum class MediaStatus {
    @SerialName("none") None,
    @SerialName("pending") Pending,
    @SerialName("downloaded") Downloaded,
    @SerialName("failed") Failed,
    @SerialName("too_large") TooLarge,
    @SerialName("skipped") Skipped,
    @SerialName("view_once") ViewOnce,
    @SerialName("unavailable") Unavailable,
}

@Serializable
data class MediaInfo(
    val status: MediaStatus = MediaStatus.None,
    val url: String? = null,
    val mime: String? = null,
    val filename: String? = null,
    val size: Long? = null,
    val width: Double? = null,
    val height: Double? = null,
    val durationSec: Double? = null,
    /** Not sent by insta_logger (kept so the shared media UI compiles; always null). */
    val thumbDataUrl: String? = null,
)

@Serializable
data class QuotedSnapshot(
    val id: String? = null,
    val senderName: String? = null,
    val fromMe: Boolean = false,
    val type: MessageType = MessageType.Unknown,
    val text: String? = null,
)

@Serializable data class Reaction(val senderId: String, val senderName: String? = null, val emoji: String, val ts: Long = 0, val fromMe: Boolean = false)

/** A shared post/reel/story/profile/link (Instagram's "XMA" attachments). */
@Serializable
data class ShareInfo(
    val kind: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val caption: String? = null,
    val url: String? = null,
    val eyebrow: String? = null,
)

@Serializable data class MusicInfo(val title: String? = null, val artist: String? = null)
@Serializable data class Mention(val id: String, val name: String? = null)
@Serializable data class AlbumPos(val index: Int = 0, val count: Int = 1)

@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String? = null,
    val senderName: String? = null,
    val senderUsername: String? = null,
    val fromMe: Boolean = false,
    val ts: Long = 0,
    val type: MessageType = MessageType.Unknown,
    val text: String? = null,
    val media: MediaInfo? = null,
    val quoted: QuotedSnapshot? = null,
    val share: ShareInfo? = null,
    val music: MusicInfo? = null,
    val mentions: List<Mention> = emptyList(),
    val forwarded: Boolean = false,
    val album: AlbumPos? = null,
    /** Disappearing message: when Instagram removes it. */
    val expiresAt: Long? = null,
    /** 1 sent · 3 seen (own messages only). */
    val ack: Int? = null,
    val reactions: List<Reaction> = emptyList(),
    val edited: Boolean = false,
    val editedAt: Long? = null,
    val editCount: Int = 0,
    /** Unsent by the sender (or gone from Instagram): the content above is the logged copy. */
    val deletedAt: Long? = null,
    /** "sender" (unsent live) · "missing" (gone, noticed by a re-check). */
    val deletedBy: String? = null,
    val source: String = "live",
    val capturedAt: Long = 0,
)

@Serializable
data class MessagePage(val messages: List<Message> = emptyList(), val nextBefore: String? = null, val nextAfter: String? = null)

@Serializable data class MessageEdit(val body: String? = null, val capturedAt: Long = 0)

@Serializable data class ChatRef(val id: String, val name: String = "", val kind: ChatKind = ChatKind.User)
@Serializable data class DeletedFeedItem(val chat: ChatRef, val message: Message)
@Serializable data class DeletedFeedPage(val items: List<DeletedFeedItem> = emptyList(), val nextBefore: String? = null)
@Serializable data class MediaFeedItem(val chat: ChatRef, val message: Message)
@Serializable data class MediaFeedPage(val items: List<MediaFeedItem> = emptyList(), val cursor: String = "0", val more: Boolean = false)
@Serializable data class SearchHit(val chat: ChatRef, val message: Message)
@Serializable data class SearchResponse(val hits: List<SearchHit> = emptyList(), val truncated: Boolean = false)

/* ------------------------------------------------------------------ events */

@Serializable
sealed class ServerEvent {
    @Serializable @SerialName("ig_state")
    data class IgStateChanged(val status: IgStatus) : ServerEvent()

    @Serializable @SerialName("message")
    data class NewMessage(val chatId: String, val messageId: String) : ServerEvent()

    @Serializable @SerialName("message_update")
    data class MessageUpdate(val chatId: String, val messageId: String, val reason: String = "") : ServerEvent()

    @Serializable @SerialName("chat_update")
    data class ChatUpdate(val chatId: String) : ServerEvent()

    @Serializable @SerialName("sync_progress")
    data class SyncProgressChanged(val sync: SyncProgress? = null) : ServerEvent()

    @Serializable @SerialName("session_revoked")
    data object SessionRevoked : ServerEvent()
}

/* ---------------------------------------------------------------- settings */

@Serializable
data class Settings(
    val downloadHistoryMedia: Boolean = true,
    val mediaMaxMb: Int = 100,
    val historyPerChat: Int = 100,
    val readReceipts: Boolean = false,
    val sessionRefresh: Boolean = true,
    val sessionRefreshHours: Int = 2,
    val storyLogging: Boolean = false,
    val storyRetentionDays: Int = 30,
    val storyCheckMinutes: Int = 45,
    /** "all" | "selected" */
    val storyAccounts: String = "all",
    val storySelected: List<String> = emptyList(),
)

@Serializable
data class SettingsPatch(
    val downloadHistoryMedia: Boolean? = null,
    val mediaMaxMb: Int? = null,
    val historyPerChat: Int? = null,
    val readReceipts: Boolean? = null,
    val sessionRefresh: Boolean? = null,
    val sessionRefreshHours: Int? = null,
    val storyLogging: Boolean? = null,
    val storyRetentionDays: Int? = null,
    val storyCheckMinutes: Int? = null,
    val storyAccounts: String? = null,
    val storySelected: List<String>? = null,
)

/* ----------------------------------------------------------------- sending */

@Serializable data class SendTextRequest(val text: String, val quotedId: String? = null)
@Serializable data class SendResult(val id: String? = null)
@Serializable data class UploadResponse(val uploadId: String, val size: Long = 0)

/** auto: photo/video/audio by type · voice: voice message */
@Serializable
enum class SendMediaKind {
    @SerialName("auto") Auto,
    @SerialName("voice") Voice,
}

@Serializable data class SendMediaRequest(val uploadId: String, val kind: SendMediaKind, val quotedId: String? = null)
@Serializable data class ReactRequest(val emoji: String)
@Serializable data class EditRequest(val text: String)

@Serializable data class AuditEntry(val id: Long, val ts: Long = 0, val event: String = "", val ip: String? = null, val detail: String? = null)
@Serializable data class WipeRequest(val password: String, val totp: String? = null)

/* -------------------------------------------------------------------- reels */

@Serializable
data class WebUser(
    val id: String,
    val username: String,
    val name: String? = null,
    val avatar: String? = null,
    val verified: Boolean = false,
    val private: Boolean = false,
    val following: Boolean? = null,
    val hasStory: Boolean = false,
)

@Serializable
data class WebMedia(
    /** "image" | "video" */
    val kind: String = "image",
    val src: String,
    val poster: String? = null,
    val width: Double? = null,
    val height: Double? = null,
)

@Serializable
data class WebPost(
    val id: String,
    val code: String? = null,
    val user: WebUser,
    val takenAt: Long = 0,
    val caption: String? = null,
    val likeCount: Long? = null,
    val commentCount: Long? = null,
    val liked: Boolean = false,
    val media: List<WebMedia> = emptyList(),
    val isReel: Boolean = false,
    val location: String? = null,
) {
    val video: WebMedia? get() = media.firstOrNull { it.kind == "video" } ?: media.firstOrNull()
    val link: String? get() = code?.let { "https://www.instagram.com/reel/$it/" }
}

@Serializable
data class WebComment(
    val id: String,
    val user: WebUser,
    val text: String = "",
    val createdAt: Long = 0,
    val likeCount: Long = 0,
    val replyCount: Long = 0,
    val replies: List<WebComment> = emptyList(),
)

@Serializable data class ReelPage(val items: List<WebPost> = emptyList(), val next: String? = null)
@Serializable data class CommentPage(val items: List<WebComment> = emptyList(), val next: String? = null)
@Serializable data class LikeRequest(val like: Boolean)
@Serializable data class CommentRequest(val text: String, val replyTo: String? = null)
@Serializable data class CommentResponse(val comment: WebComment? = null)

/* ------------------------------------------------------------------ stories */

@Serializable
data class StoryAuthor(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val count: Int = 0,
    val latest: Long = 0,
)

@Serializable
data class StoryItem(
    val id: String,
    val userId: String,
    val username: String? = null,
    /** "image" | "video" */
    val type: String = "image",
    val takenAt: Long = 0,
    val expiresAt: Long? = null,
    /** pending · downloaded · failed · too_large */
    val status: String = "pending",
    val url: String? = null,
)

@Serializable data class StoryPage(val items: List<StoryItem> = emptyList(), val nextBefore: Long? = null)

@Serializable
data class StoryStatus(
    val enabled: Boolean = false,
    val retentionDays: Int = 30,
    val stories: Int = 0,
    val lastCheckAt: Long? = null,
    val nextCheckAt: Long? = null,
    val lastResult: String? = null,
)

/** An account seen in the stories bar (for picking whose stories are logged). */
@Serializable
data class StoryAccount(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val count: Int = 0,
    val selected: Boolean = false,
)
