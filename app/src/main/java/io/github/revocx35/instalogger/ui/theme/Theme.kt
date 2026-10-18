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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Design tokens of the web UI (`web/src/styles.css` :root and its dark-mode override), so the app
 * looks the same. Light/dark follows the system, like the web UI.
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
    val authBanner: Color,
    val avatarIconBg: Color,
    val avatarIconFg: Color,
    val shadow: Color,
    val isDark: Boolean,
)

val LightWaColors = WaColors(
    bg = Color(0xFFFAFAFA),
    panel = Color(0xFFFFFFFF),
    panel2 = Color(0xFFFAFAFA),
    head = Color(0xFFFFFFFF),
    rail = Color(0xFFFFFFFF),
    chatBg = Color(0xFFFFFFFF),
    bubbleIn = Color(0xFFEFEFEF),
    bubbleOut = Color(0xFF3797F0),
    bubbleOutText = Color(0xFFFFFFFF),
    text = Color(0xFF0C1014),
    text2 = Color(0xFF3D4651),
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
    authBanner = Color(0xFFD62976),
    avatarIconBg = Color(0xFF737373),
    avatarIconFg = Color(0xFFEFEFEF),
    shadow = Color(0x00000000),
    isDark = false,
)

val DarkWaColors = WaColors(
    bg = Color(0xFF000000),
    panel = Color(0xFF000000),
    panel2 = Color(0xFF121212),
    head = Color(0xFF000000),
    rail = Color(0xFF000000),
    chatBg = Color(0xFF000000),
    bubbleIn = Color(0xFF262626),
    bubbleOut = Color(0xFF3797F0),
    bubbleOutText = Color(0xFFFFFFFF),
    text = Color(0xFFF5F5F5),
    text2 = Color(0xFFC7C7C7),
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
    authBanner = Color(0xFF962FBF),
    avatarIconBg = Color(0xFF363636),
    avatarIconFg = Color(0xFFDBDBDB),
    shadow = Color(0x00000000),
    isDark = true,
)

val LocalWaColors = staticCompositionLocalOf { LightWaColors }

/** Type scale of the web UI (15px base). */
object WaType {
    val base = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontFamily = FontFamily.SansSerif)
    val bubble = TextStyle(fontSize = 14.5.sp, lineHeight = 20.sp)
    val small = TextStyle(fontSize = 12.75.sp, lineHeight = 17.sp)
    val tiny = TextStyle(fontSize = 11.25.sp, lineHeight = 14.sp)
    val meta = TextStyle(fontSize = 10.5.sp, lineHeight = 13.sp)
    val chatName = TextStyle(fontSize = 15.5.sp, lineHeight = 21.sp)
    val paneTitle = TextStyle(fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
    val pageTitle = TextStyle(fontSize = 22.5.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
    val sectionTitle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val authTitle = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)
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
