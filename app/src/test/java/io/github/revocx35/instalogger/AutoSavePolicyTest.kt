package io.github.revocx35.instalogger

import io.github.revocx35.instalogger.core.ChatKind
import io.github.revocx35.instalogger.core.ChatRef
import io.github.revocx35.instalogger.core.MediaFeedItem
import io.github.revocx35.instalogger.core.MediaInfo
import io.github.revocx35.instalogger.core.MediaStatus
import io.github.revocx35.instalogger.core.Message
import io.github.revocx35.instalogger.core.MessageType
import io.github.revocx35.instalogger.media.AutoSavePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSavePolicyTest {
    private fun item(type: MessageType, kind: ChatKind = ChatKind.User, status: MediaStatus = MediaStatus.Downloaded, url: String? = "/api/media/7", expiresAt: Long? = null) =
        MediaFeedItem(
            ChatRef("1", "A", kind),
            Message(id = "m", chatId = "1", type = type, expiresAt = expiresAt, media = MediaInfo(status = status, url = url, mime = "image/jpeg")),
        )

    @Test
    fun onlyDisappearingSavesOnlyMessagesInstagramRemovesLater() {
        assertTrue(AutoSavePolicy.wants(item(MessageType.Image, expiresAt = 1_800_000_000_000), videos = false, status = false, onlyDisappearing = true))
        assertFalse(AutoSavePolicy.wants(item(MessageType.Image), videos = false, status = false, onlyDisappearing = true))
        assertTrue(AutoSavePolicy.wants(item(MessageType.Image), videos = false, status = false, onlyDisappearing = false))
        assertFalse(AutoSavePolicy.wants(item(MessageType.Video, expiresAt = 1_800_000_000_000), videos = false, status = false, onlyDisappearing = true))
    }

    @Test
    fun savesPhotosOnlyByDefault() {
        assertTrue(AutoSavePolicy.wants(item(MessageType.Image), videos = false, status = false))
        assertTrue(AutoSavePolicy.wants(item(MessageType.Image, ChatKind.Group), videos = false, status = false))
        for (t in listOf(MessageType.Video, MessageType.Gif, MessageType.Sticker, MessageType.Share, MessageType.Voice)) {
            assertFalse(t.name, AutoSavePolicy.wants(item(t), videos = false, status = false))
        }
        assertTrue(AutoSavePolicy.wants(item(MessageType.Video), videos = true, status = false))
        assertTrue(AutoSavePolicy.wants(item(MessageType.Gif), videos = true, status = false))
        assertFalse(AutoSavePolicy.wants(item(MessageType.Sticker), videos = true, status = true))
    }

    @Test
    fun skipsAnythingNotDownloaded() {
        assertFalse(AutoSavePolicy.wants(item(MessageType.Image, status = MediaStatus.Failed), videos = true, status = true))
        assertFalse(AutoSavePolicy.wants(item(MessageType.Image, url = null), videos = true, status = true))
    }

    @Test
    fun cursorIsTheMediaId() {
        assertEquals("7", AutoSavePolicy.cursorOf(item(MessageType.Image)))
        assertNull(AutoSavePolicy.cursorOf(item(MessageType.Image, url = "/api/media/x")))
        assertNull(AutoSavePolicy.cursorOf(item(MessageType.Image, url = null)))
    }
}
