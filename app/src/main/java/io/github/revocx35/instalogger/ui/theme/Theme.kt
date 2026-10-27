package io.github.revocx35.instalogger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.revocx35.instalogger.R

/**
 * Design tokens of the web UI (`web/src/styles.css` :root and its dark-mode override), styled after Instagram's own
 * app, so both look the same. Light/dark follows the system, like the web UI.
 */
@Immutable
data class WaColors(
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val head: Color,
    val rail: Color,
    val chatBg: Color,
    val bubbleIn: Color,
    val bubbleOut: Color,
    /** Text on the owner's (blue) bubbles. */
    val bubbleOutText: Color,
    val text: Color,
    val text2: Color,
    val muted: Color,
    val border: Color,
    val hover: Color,
    val active: Color,
    val accent: Color,
    val accent2: Color,
    val accentText: Color,
    val link: Color,
    val danger: Color,
    val dangerBg: Color,
    val warn: Color,
    val warnBg: Color,
    val ok: Color,
    val system: Color,
    val quoteBg: Color,
    val tickRead: Color,
    /** Accent used for text on neutral backgrounds (chip label, quote sender): accent-2 light, ok dark. */
    val accentOnPanel: Color,
    val avatarIconBg: Color,
    val avatarIconFg: Color,
    val shadow: Color,
    /** Instagram's like red (filled hearts, notification dots). */
    val like: Color,
    /** Neutral buttons (Instagram's grey "secondary" button). */
    val btn: Color,
    /** Background of text fields on auth screens. */
    val inputBg: Color,
    /** Sheets and menus (lighter than the page in dark mode, like Instagram). */
    val sheet: Color,
    val isDark: Boolean,
)

val LightWaColors = WaColors(
    bg = Color(0xFFFFFFFF),
    panel = Color(0xFFFFFFFF),
    panel2 = Color(0xFFEFEFEF),
    head = Color(0xFFFFFFFF),
    rail = Color(0xFFFFFFFF),
    chatBg = Color(0xFFFFFFFF),
    bubbleIn = Color(0xFFEFEFEF),
    bubbleOut = Color(0xFF3797F0),
    bubbleOutText = Color(0xFFFFFFFF),
    text = Color(0xFF000000),
    text2 = Color(0xFF262626),
    muted = Color(0xFF737373),
    border = Color(0xFFDBDBDB),
    hover = Color(0xFFF2F2F2),
    active = Color(0xFFEFEFEF),
    accent = Color(0xFF0095F6),
    accent2 = Color(0xFF1877F2),
    accentText = Color(0xFFFFFFFF),
    link = Color(0xFF00376B),
    danger = Color(0xFFED4956),
    dangerBg = Color(0xFFFDEAEC),
    warn = Color(0xFFF5A623),
    warnBg = Color(0xFFFFF4D6),
    ok = Color(0xFF58C322),
    system = Color(0x00FFFFFF),
    quoteBg = Color(0x0F000000),
    tickRead = Color(0xFF0095F6),
    accentOnPanel = Color(0xFF1877F2),
    avatarIconBg = Color(0xFFC7C7C7),
    avatarIconFg = Color(0xFFFFFFFF),
    shadow = Color(0x00000000),
    like = Color(0xFFFF3040),
    btn = Color(0xFFEFEFEF),
    inputBg = Color(0xFFFAFAFA),
    sheet = Color(0xFFFFFFFF),
    isDark = false,
)

val DarkWaColors = WaColors(
    bg = Color(0xFF000000),
    panel = Color(0xFF000000),
    panel2 = Color(0xFF262626),
    head = Color(0xFF000000),
    rail = Color(0xFF000000),
    chatBg = Color(0xFF000000),
    bubbleIn = Color(0xFF262626),
    bubbleOut = Color(0xFF3797F0),
    bubbleOutText = Color(0xFFFFFFFF),
    text = Color(0xFFF5F5F5),
    text2 = Color(0xFFE0E0E0),
    muted = Color(0xFFA8A8A8),
    border = Color(0xFF262626),
    hover = Color(0xFF1A1A1A),
    active = Color(0xFF262626),
    accent = Color(0xFF0095F6),
    accent2 = Color(0xFF1877F2),
    accentText = Color(0xFFFFFFFF),
    link = Color(0xFFE0F1FF),
    danger = Color(0xFFFF6170),
    dangerBg = Color(0xFF3A1418),
    warn = Color(0xFFF5A623),
    warnBg = Color(0xFF3B3218),
    ok = Color(0xFF58C322),
    system = Color(0x00000000),
    quoteBg = Color(0x14FFFFFF),
    tickRead = Color(0xFF0095F6),
    accentOnPanel = Color(0xFF0095F6),
    avatarIconBg = Color(0xFF363636),
    avatarIconFg = Color(0xFFDBDBDB),
    shadow = Color(0x00000000),
    like = Color(0xFFFF3040),
    btn = Color(0xFF363636),
    inputBg = Color(0xFF121212),
    sheet = Color(0xFF262626),
    isDark = true,
)

val LocalWaColors = staticCompositionLocalOf { LightWaColors }

/** The script face of the wordmark (Grand Hotel, SIL OFL — `third_party/GrandHotel-OFL.txt`). */
val ScriptFont = FontFamily(Font(R.font.grand_hotel))

/** Type scale of the web UI, which follows Instagram's (14 sp base, 15 sp messages, bold titles). */
object WaType {
    val base = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontFamily = FontFamily.SansSerif)
    val bubble = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
    val small = TextStyle(fontSize = 12.5.sp, lineHeight = 16.sp)
    val tiny = TextStyle(fontSize = 11.sp, lineHeight = 14.sp)
    val meta = TextStyle(fontSize = 10.5.sp, lineHeight = 13.sp)
    val chatName = TextStyle(fontSize = 14.sp, lineHeight = 18.sp)
    val paneTitle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
    val pageTitle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
    val sectionTitle = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
    val authTitle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val wordmark = TextStyle(fontFamily = ScriptFont, fontSize = 30.sp, lineHeight = 34.sp)
    val mono = TextStyle(fontFamily = FontFamily.Monospace)
}

object Wa {
    val colors: WaColors
        @Composable get() = LocalWaColors.current
}

@Composable
fun WaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) DarkWaColors else LightWaColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.accentText, secondary = c.accent2, background = c.bg, onBackground = c.text,
            surface = c.panel, onSurface = c.text, surfaceVariant = c.panel2, onSurfaceVariant = c.text2, outline = c.border,
            error = c.danger, surfaceContainerHigh = c.panel, surfaceContainerHighest = c.panel2, surfaceContainer = c.panel,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.accentText, secondary = c.accent2, background = c.bg, onBackground = c.text,
            surface = c.panel, onSurface = c.text, surfaceVariant = c.panel2, onSurfaceVariant = c.text2, outline = c.border,
            error = c.danger, surfaceContainerHigh = c.panel, surfaceContainerHighest = c.panel2, surfaceContainer = c.panel,
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalWaColors provides c,
            androidx.compose.foundation.text.selection.LocalTextSelectionColors provides TextSelectionColors(c.accent, c.accent.copy(alpha = 0.3f)),
            androidx.compose.material3.LocalTextStyle provides WaType.base.copy(color = c.text),
            androidx.compose.material3.LocalContentColor provides c.text,
            content = content,
        )
    }
}
