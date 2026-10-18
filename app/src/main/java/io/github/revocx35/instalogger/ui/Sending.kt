package io.github.revocx35.instalogger.ui

import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.revocx35.instalogger.core.SendMediaKind

/** A file picked or recorded to be sent ([size] is -1 when unknown). */
class OutgoingFile(val uri: Uri, val name: String?, val mime: String, val size: Long)

/**
 * Sending as the linked Instagram account. What is sent comes back through the live updates like any other
 * message, so callers don't add anything to the chat themselves. Errors are thrown as ApiException with a
 * message that can be shown as is.
 */
interface MessageSender {
    suspend fun sendText(chatId: String, text: String, quotedId: String?)
    suspend fun sendFile(chatId: String, file: OutgoingFile, kind: SendMediaKind, caption: String?, quotedId: String?)

    /** An empty [emoji] removes the reaction. */
    suspend fun react(messageId: String, emoji: String)

    /** Unsend (own messages). */
    suspend fun unsend(messageId: String)
    suspend fun edit(messageId: String, text: String)
    suspend fun markRead(chatId: String)


    /** Whether opening a chat should send read receipts (a setting, off by default). */
    suspend fun readReceipts(): Boolean
}

/** Null where sending isn't available (screenshot tests, previews). */
val LocalSender = staticCompositionLocalOf<MessageSender?> { null }

/** Largest file the server accepts. */
const val SEND_MAX_BYTES = 64L * 1024 * 1024
