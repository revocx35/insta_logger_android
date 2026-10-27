package io.github.revocx35.instalogger.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The web UI's Instagram-style outline icons (`web/src/components/Icon.tsx`: 24 px grid, 2 px round stroke), same
 * paths, plus a few solid extras the native client needs. Generated from the web source; tint them with `Icon(tint = …)`.
 */
object WaIcons {
    val chats: ImageVector by lazy { stroke("chats", "M12 2.8c5.2 0 9.2 3.8 9.2 8.8s-4 8.8-9.2 8.8c-1 0-1.9-.1-2.8-.4L5.5 21.5l.4-3.3C3.9 16.6 2.8 14.3 2.8 11.6 2.8 6.6 6.8 2.8 12 2.8z", "m7.2 14 3.3-4 2.6 2 3.7-3.7-3.3 4-2.6-2z") }
    val search: ImageVector by lazy { stroke("search", "M10.5 3.5a7 7 0 1 1 0 14 7 7 0 0 1 0-14z", "M15.6 15.6 21 21") }
    val reels: ImageVector by lazy { stroke("reels", "M3.5 7.5h17", "M8.5 3 11 7.5", "M14.5 3 17 7.5", "M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4z", "M10 11v6l5-3z") }
    val stories: ImageVector by lazy { stroke("stories", "M12 3a9 9 0 1 1-6.4 2.7", "M3.4 9.2a9 9 0 0 1 .9-2", "M12 8v8", "M8 12h8") }
    val deleted: ImageVector by lazy { stroke("deleted", "M3.5 6h17", "M9 6V4.5A1.5 1.5 0 0 1 10.5 3h3A1.5 1.5 0 0 1 15 4.5V6", "M5.5 6l1 13.5A1.7 1.7 0 0 0 8.2 21h7.6a1.7 1.7 0 0 0 1.7-1.5l1-13.5", "M10 10.5v6", "M14 10.5v6") }
    val monitor: ImageVector by lazy { stroke("monitor", "M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18z", "M3.5 12h17", "M12 3c2.4 2.5 3.6 5.5 3.6 9s-1.2 6.5-3.6 9c-2.4-2.5-3.6-5.5-3.6-9s1.2-6.5 3.6-9z") }
    val settings: ImageVector by lazy { stroke("settings", "M12 9a3 3 0 1 1 0 6 3 3 0 0 1 0-6z", "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z") }
    val menu: ImageVector by lazy { stroke("menu", "M3.5 6h17", "M3.5 12h17", "M3.5 18h17") }
    val logout: ImageVector by lazy { stroke("logout", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4", "M16 17l5-5-5-5", "M21 12H9") }
    val group: ImageVector by lazy { stroke("group", "M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", "M2 21v-1.5A5.5 5.5 0 0 1 7.5 14h3a5.5 5.5 0 0 1 5.5 5.5V21", "M16 3.2a4 4 0 0 1 0 7.6", "M22 21v-1.5a5.5 5.5 0 0 0-3.5-5.1") }
    val user: ImageVector by lazy { stroke("user", "M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", "M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1") }
    val heart: ImageVector by lazy { stroke("heart", "M16.8 3.5c2.7 0 4.7 2.2 4.7 5.1 0 4.6-4.6 8.4-9.5 12-4.9-3.6-9.5-7.4-9.5-12 0-2.9 2-5.1 4.7-5.1 2 0 3.5 1.1 4.8 3 1.3-1.9 2.8-3 4.8-3z") }
    val heartFill: ImageVector by lazy { solid("heartFill", "M16.8 3.5c2.7 0 4.7 2.2 4.7 5.1 0 4.6-4.6 8.4-9.5 12-4.9-3.6-9.5-7.4-9.5-12 0-2.9 2-5.1 4.7-5.1 2 0 3.5 1.1 4.8 3 1.3-1.9 2.8-3 4.8-3z") }
    val comment: ImageVector by lazy { stroke("comment", "M20.7 16.8A9.3 9.3 0 1 0 17 20.6L21.5 21.5z") }
    val send: ImageVector by lazy { stroke("send", "M22 3 9.2 10.1", "M22 3l-7.1 18-5.7-10.9L2 6.3z") }
    val bookmark: ImageVector by lazy { stroke("bookmark", "M19 21l-7-5.8L5 21V4a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1z") }
    val more: ImageVector by lazy { solid("more", "M12 4.5a1.8 1.8 0 1 1 0 3.6 1.8 1.8 0 0 1 0-3.6zm0 5.7a1.8 1.8 0 1 1 0 3.6 1.8 1.8 0 0 1 0-3.6zm0 5.7a1.8 1.8 0 1 1 0 3.6 1.8 1.8 0 0 1 0-3.6z") }
    val moreH: ImageVector by lazy { solid("moreH", "M4.5 12a1.8 1.8 0 1 1 3.6 0 1.8 1.8 0 0 1-3.6 0zm5.7 0a1.8 1.8 0 1 1 3.6 0 1.8 1.8 0 0 1-3.6 0zm5.7 0a1.8 1.8 0 1 1 3.6 0 1.8 1.8 0 0 1-3.6 0z") }
    val back: ImageVector by lazy { stroke("back", "M15 4.5 7.5 12l7.5 7.5") }
    val close: ImageVector by lazy { stroke("close", "M5 5l14 14", "M19 5 5 19") }
    val check: ImageVector by lazy { stroke("check", "M4.5 12.5l5 5 10-11") }
    val checks: ImageVector by lazy { stroke("checks", "M2 12.5l5 5 10-11", "M12.5 16.5l1 1 10-11") }
    val plus: ImageVector by lazy { stroke("plus", "M12 4.5v15", "M4.5 12h15") }
    val edit: ImageVector by lazy { stroke("edit", "M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4z") }
    val reply: ImageVector by lazy { stroke("reply", "M9.5 14.5 4 9l5.5-5.5", "M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5V20") }
    val forward: ImageVector by lazy { stroke("forward", "M14.5 14.5 20 9l-5.5-5.5", "M20 9H9.5A5.5 5.5 0 0 0 4 14.5V20") }
    val refresh: ImageVector by lazy { stroke("refresh", "M20.5 12a8.5 8.5 0 1 1-2.5-6", "M20.5 3.5V8h-4.5") }
    val download: ImageVector by lazy { stroke("download", "M12 3.5v12", "M7 10.5l5 5 5-5", "M4 20.5h16") }
    val link: ImageVector by lazy { stroke("link", "M10 13.5a4.5 4.5 0 0 0 6.4.4l3-3a4.5 4.5 0 0 0-6.4-6.4l-1.7 1.7", "M14 10.5a4.5 4.5 0 0 0-6.4-.4l-3 3a4.5 4.5 0 0 0 6.4 6.4l1.7-1.7") }
    val pushpin: ImageVector by lazy { stroke("pushpin", "M15 3.5 20.5 9l-2 1-3.2 3.2.4 4.3L14 19.2 4.8 10l1.7-1.7 4.3.4L14 5.5z", "M9.4 14.6 3.5 20.5") }
    val image: ImageVector by lazy { stroke("image", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M8.5 7a1.5 1.5 0 1 1 0 3 1.5 1.5 0 0 1 0-3z", "m21 15-5-5L5 21") }
    val video: ImageVector by lazy { stroke("video", "M4 6h10a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z", "M16 10.5 22 7v10l-6-3.5") }
    val mic: ImageVector by lazy { stroke("mic", "M12 3a3 3 0 0 1 3 3v5.5a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3z", "M5.5 11a6.5 6.5 0 0 0 13 0", "M12 17.5V21") }
    val smile: ImageVector by lazy { stroke("smile", "M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18z", "M8.5 14.5a4.5 4.5 0 0 0 7 0", "M9 9.5h.01", "M15 9.5h.01") }
    val attach: ImageVector by lazy { stroke("attach", "M20.5 11.5l-8.4 8.4a5.3 5.3 0 0 1-7.5-7.5l8.4-8.4a3.5 3.5 0 0 1 5 5l-8.4 8.4a1.8 1.8 0 0 1-2.5-2.5l7.8-7.8") }
    val file: ImageVector by lazy { stroke("file", "M14 3H6.5A1.5 1.5 0 0 0 5 4.5v15A1.5 1.5 0 0 0 6.5 21h11a1.5 1.5 0 0 0 1.5-1.5V8z", "M14 3v5h5") }
    val music: ImageVector by lazy { stroke("music", "M9 18V5l11-2v13", "M6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6z", "M17 19a3 3 0 1 0 0-6 3 3 0 0 0 0 6z") }
    val volume: ImageVector by lazy { stroke("volume", "M11 5 6 9H3v6h3l5 4z", "M15.5 8.5a5 5 0 0 1 0 7", "M18.5 5.5a9 9 0 0 1 0 13") }
    val mute: ImageVector by lazy { stroke("mute", "M11 5 6 9H3v6h3l5 4z", "M22 9l-6 6", "M16 9l6 6") }
    val play: ImageVector by lazy { solid("play", "M7 4.5v15l13-7.5z") }
    val pause: ImageVector by lazy { solid("pause", "M6 4h4v16H6zm8 0h4v16h-4z") }
    val clock: ImageVector by lazy { stroke("clock", "M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18z", "M12 7v5l3 2") }
    val eye: ImageVector by lazy { stroke("eye", "M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z", "M12 9a3 3 0 1 1 0 6 3 3 0 0 1 0-6z") }
    val lock: ImageVector by lazy { stroke("lock", "M6 11h12a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-8a1 1 0 0 1 1-1z", "M8 11V7a4 4 0 0 1 8 0v4") }
    val shield: ImageVector by lazy { stroke("shield", "M12 3 4.5 6v5.5c0 4.5 3.2 8.4 7.5 9.5 4.3-1.1 7.5-5 7.5-9.5V6z", "m8.8 12 2.2 2.2 4.2-4.4") }
    val warning: ImageVector by lazy { stroke("warning", "M10.3 4 2.4 18a2 2 0 0 0 1.7 3h15.8a2 2 0 0 0 1.7-3L13.7 4a2 2 0 0 0-3.4 0z", "M12 9.5v4", "M12 17h.01") }
    val ban: ImageVector by lazy { stroke("ban", "M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18z", "M5.6 5.6l12.8 12.8") }
    val verified: ImageVector by lazy { solid("verified", "M12 1.8l2.5 1.9 3.1-.2.9 3 2.6 1.8-1 3 1 2.9-2.6 1.8-.9 3-3.1-.2L12 22.2l-2.5-1.9-3.1.2-.9-3-2.6-1.8 1-2.9-1-3 2.6-1.8.9-3 3.1.2zm-1.2 13.6 5.5-5.6-1.3-1.3-4.2 4.3-2.2-2.2-1.3 1.3z") }
    val pin: ImageVector by lazy { solid("pin", "M12 2a7 7 0 0 1 7 7c0 5-7 13-7 13S5 14 5 9a7 7 0 0 1 7-7zm0 4a3 3 0 1 0 0 6 3 3 0 0 0 0-6z") }
    val phone: ImageVector by lazy { solid("phone", "M6.6 10.8a15 15 0 0 0 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.2 1.1.4 2.3.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1A17 17 0 0 1 3 4c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.5.6 3.6.1.3 0 .7-.2 1l-2.3 2.2z") }
    val archive: ImageVector by lazy { solid("archive", "M3 3h18v5H3V3zm1 6h16v11a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V9zm5 3v2h6v-2H9z") }
    val poll: ImageVector by lazy { solid("poll", "M4 4h2v16H4V4zm5 6h2v10H9V10zm5-4h2v14h-2V6zm5 8h2v6h-2v-6z") }
    val keyboard: ImageVector by lazy { solid("keyboard", "M20 5H4c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm-9 3h2v2h-2V8zm0 3h2v2h-2v-2zM8 8h2v2H8V8zm0 3h2v2H8v-2zm-1 2H5v-2h2v2zm0-3H5V8h2v2zm9 7H8v-2h8v2zm0-4h-2v-2h2v2zm0-3h-2V8h2v2zm3 3h-2v-2h2v2zm0-3h-2V8h2v2z") }
    val openNew: ImageVector by lazy { solid("openNew", "M19 19H5V5h7V3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z") }
    val server: ImageVector by lazy { solid("server", "M20 13H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1zM7 19c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zM20 3H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1V4c0-.55-.45-1-1-1zM7 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z") }
    val copy: ImageVector by lazy { solid("copy", "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z") }
    val chevronDown: ImageVector by lazy { solid("chevronDown", "M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z") }
    val backspace: ImageVector by lazy { solid("backspace", "M22 3H7c-.69 0-1.23.35-1.59.88L0 12l5.41 8.11c.36.53.9.89 1.59.89h15c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-3 12.59L17.59 17 14 13.41 10.41 17 9 15.59 12.59 12 9 8.41 10.41 7 14 10.59 17.59 7 19 8.41 15.41 12 19 15.59z") }
    val enter: ImageVector by lazy { solid("enter", "M19 7v4H5.83l3.58-3.59L8 6l-6 6 6 6 1.41-1.41L5.83 13H21V7z") }
    val callEnd: ImageVector by lazy { solid("callEnd", "M12 9c-1.6 0-3.1.3-4.6.8v3.1c0 .4-.2.7-.6.9-1 .5-1.9 1.1-2.7 1.8-.2.2-.4.3-.7.3s-.5-.1-.7-.3L.3 13.2a1 1 0 0 1 0-1.4C3.4 8.9 7.5 7 12 7s8.6 1.9 11.7 4.8a1 1 0 0 1 0 1.4l-2.5 2.5c-.2.2-.4.3-.7.3s-.5-.1-.7-.3c-.8-.7-1.7-1.3-2.7-1.8a1 1 0 0 1-.6-.9V9.8C15.1 9.3 13.6 9 12 9z") }
    val micOff: ImageVector by lazy { solid("micOff", "M19 11h-1.7c0 .7-.2 1.4-.4 2l1.2 1.2c.6-.9.9-2 .9-3.2zm-4-.2V5a3 3 0 0 0-5.9-.7L15 10.8zM4.3 3 3 4.3l6 6V11a3 3 0 0 0 4.4 2.6l1.6 1.7A5 5 0 0 1 7 11H5a7 7 0 0 0 6 6.9V21h2v-3.1c.9-.1 1.8-.4 2.5-.9l4.2 4.3L21 20 4.3 3z") }
    val speaker: ImageVector by lazy { solid("speaker", "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z") }
    val videoOff: ImageVector by lazy { solid("videoOff", "M3.3 2 2 3.3 4.7 6H3a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h12c.2 0 .4-.1.6-.2l3.1 3.1 1.3-1.3L3.3 2zM21 7l-5 3.5V7a1 1 0 0 0-1-1H9.8L21 17.2V7z") }
    val zoomIn: ImageVector by lazy { solid("zoomIn", "M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14zm.5-7H9v2H7v1h2v2h1v-2h2V9h-2z") }

    private fun builder(name: String) =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)

    /** Outline icon: each path stroked 2 px with round caps and joins (like the web's `fill="none"` icons). */
    private fun stroke(name: String, vararg d: String): ImageVector {
        val b = builder(name)
        for (p in d) {
            b.addPath(
                pathData = addPathNodes(p),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    private fun solid(name: String, vararg d: String): ImageVector {
        val b = builder(name)
        for (p in d) b.addPath(pathData = addPathNodes(p), fill = SolidColor(Color.Black))
        return b.build()
    }
}
