package io.github.revocx35.instalogger.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.revocx35.instalogger.core.IgState
import io.github.revocx35.instalogger.core.IgStatus
import io.github.revocx35.instalogger.ui.theme.Wa
import io.github.revocx35.instalogger.ui.theme.WaIcons
import io.github.revocx35.instalogger.ui.theme.WaType
import kotlinx.coroutines.delay

/* Building blocks mirroring the web UI's CSS classes (.btn, .pill, .badge, .chip, .search-box, …). */

@Composable
fun Spinner(size: Dp = 20.dp, modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = Wa.colors.muted,
        trackColor = Wa.colors.muted.copy(alpha = 0.25f),
        strokeWidth = 2.dp,
    )
}

@Composable
fun WaIcon(icon: ImageVector, size: Dp = 20.dp, tint: Color = androidx.compose.material3.LocalContentColor.current, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Icon(icon, contentDescription = contentDescription, modifier = modifier.size(size), tint = tint)
}

enum class BtnStyle { Default, Primary, Danger, Ghost }

/** `.btn`: Instagram's flat 8 dp button (grey, blue primary) with the danger / ghost / tiny / block variants. */
@Composable
fun WaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BtnStyle = BtnStyle.Default,
    enabled: Boolean = true,
    tiny: Boolean = false,
    block: Boolean = false,
    busy: Boolean = false,
    icon: ImageVector? = null,
) {
    val c = Wa.colors
    val (bg, fg) = when (style) {
        BtnStyle.Primary -> c.accent to c.accentText
        BtnStyle.Danger -> c.btn to c.danger
        BtnStyle.Ghost -> Color.Transparent to c.text
        BtnStyle.Default -> c.btn to c.text
    }
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .then(if (block) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = if (tiny) 28.dp else 36.dp)
            .clip(shape)
            .background(bg, shape)
            .clickable(enabled = enabled && !busy, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.55f)
            .padding(if (tiny) PaddingValues(horizontal = 10.dp, vertical = 2.dp) else PaddingValues(horizontal = 16.dp, vertical = 7.dp)),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            Spinner(if (tiny) 14.dp else 18.dp)
        } else {
            if (icon != null) WaIcon(icon, if (tiny) 14.dp else 18.dp, fg)
            Text(text, color = fg, style = (if (tiny) WaType.small else WaType.base).copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** `.icon-btn`: 40dp round icon button (active = highlighted background). */
@Composable
fun IconBtn(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, active: Boolean = false, size: Dp = 40.dp, iconSize: Dp = 24.dp, tint: Color = Wa.colors.text, enabled: Boolean = true) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) Wa.colors.active else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) {
        WaIcon(icon, iconSize, tint, contentDescription = contentDescription)
    }
}

@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, style: TextStyle = WaType.base, color: Color = Wa.colors.link, icon: ImageVector? = null) {
    Row(modifier.clickable(role = Role.Button, onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (icon != null) WaIcon(icon, 12.dp, color)
        Text(text, style = style, color = color)
    }
}

/** Two-step confirmation built into the page (like the web's ConfirmButton; disarms after 5 s). */
@Composable
fun ConfirmButton(label: String, confirmLabel: String, onConfirm: () -> Unit, modifier: Modifier = Modifier, style: BtnStyle = BtnStyle.Default, enabled: Boolean = true) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(5000)
            armed = false
        }
    }
    if (armed) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WaButton(confirmLabel, { armed = false; onConfirm() }, style = BtnStyle.Danger, enabled = enabled)
            WaButton("Cancel", { armed = false }, style = BtnStyle.Ghost)
        }
    } else {
        WaButton(label, { armed = true }, modifier, style = style, enabled = enabled)
    }
}

private val STATE_LABEL = mapOf(
    IgState.Idle to "Not started",
    IgState.Starting to "Starting",
    IgState.Login to "Waiting for login",
    IgState.Challenge to "Needs confirmation",
    IgState.Syncing to "Importing history",
    IgState.Ready to "Connected",
    IgState.Error to "Reconnecting",
)

fun waStateLabel(status: IgStatus?): String {
    val state = status?.state ?: IgState.Idle
    val sync = status?.sync
    if (state == IgState.Syncing && sync != null) return "Importing ${sync.chatsDone}/${sync.chatsTotal}"
    return STATE_LABEL[state] ?: ""
}

/** `.pill` with a colored status dot (pulsing while busy). */
@Composable
fun WaStatePill(status: IgStatus?, compact: Boolean = false, modifier: Modifier = Modifier) {
    val c = Wa.colors
    val state = status?.state ?: IgState.Idle
    val busy = state == IgState.Syncing || state == IgState.Starting
    val dot = when {
        state == IgState.Ready -> c.ok
        busy -> c.accent
        state == IgState.Login || state == IgState.Challenge -> c.warn
        else -> c.danger
    }
    val alpha = if (busy) {
        val t = rememberInfiniteTransition(label = "pulse")
        t.animateFloat(1f, 0.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse").value
    } else {
        1f
    }
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.panel2)
            .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 6.dp else 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(9.dp).alpha(alpha).clip(CircleShape).background(dot))
        if (!compact) Text(waStateLabel(status), style = WaType.small, color = c.text, maxLines = 1)
    }
}

enum class BadgeTone { Red, Gray, Green }

@Composable
fun Badge(text: String, tone: BadgeTone, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    val c = Wa.colors
    val (bg, fg) = when (tone) {
        BadgeTone.Red -> c.like to Color.White
        BadgeTone.Gray -> c.panel2 to c.muted
        BadgeTone.Green -> c.accent to Color.White
    }
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 7.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (icon != null) WaIcon(icon, 12.dp, fg)
        Text(text, style = WaType.tiny.copy(fontWeight = FontWeight.SemiBold), color = fg)
    }
}

/** Instagram's outlined filter chip (filled grey when selected). */
@Composable
fun FilterChip(text: String, active: Boolean, onClick: () -> Unit) {
    val c = Wa.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier.clip(shape).background(if (active) c.panel2 else Color.Transparent).border(1.dp, if (active) c.panel2 else c.border, shape)
            .clickable(role = Role.Tab, onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(text, style = WaType.base.copy(fontWeight = FontWeight.SemiBold), color = c.text)
    }
}

fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t)

@Composable
fun ErrorNote(error: String?, modifier: Modifier = Modifier) {
    if (error.isNullOrBlank()) return
    Row(modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        WaIcon(WaIcons.warning, 16.dp, Wa.colors.danger)
        Text(error, color = Wa.colors.danger, style = WaType.base)
    }
}

@Composable
fun OkNote(text: String?, icon: ImageVector? = null) {
    if (text.isNullOrBlank()) return
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null) WaIcon(icon, 16.dp, Wa.colors.accentOnPanel)
        Text(text, color = if (Wa.colors.isDark) Wa.colors.ok else Wa.colors.accent2, style = WaType.base)
    }
}

/** `.note`: small explanatory box. */
@Composable
fun Note(text: String, icon: ImageVector? = null) {
    val c = Wa.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.panel2).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) WaIcon(icon, 16.dp, c.text2)
        Text(text, style = WaType.small, color = c.text2)
    }
}

/** `.banner.warn` */
@Composable
fun WarnBanner(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().background(Wa.colors.warnBg).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WaIcon(WaIcons.warning, 18.dp, Wa.colors.text)
        content()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, modifier: Modifier = Modifier, body: (@Composable () -> Unit)? = null) {
    val c = Wa.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Instagram's empty-state mark: the icon in a thin circle.
        Box(Modifier.size(96.dp).border(2.dp, c.text, CircleShape), contentAlignment = Alignment.Center) { WaIcon(icon, 44.dp, c.text) }
        Text(title, style = WaType.base.copy(fontSize = 20.sp, lineHeight = 26.sp), color = c.text, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        if (body != null) {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides c.muted) {
                Box(Modifier.widthIn(max = 460.dp)) { body() }
            }
        }
    }
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier, style: TextStyle = WaType.base, textAlign: TextAlign? = null, maxLines: Int = Int.MAX_VALUE) {
    Text(text, modifier, color = Wa.colors.muted, style = style, textAlign = textAlign, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** Labelled input like the web's `.form label > input`. */
@Composable
fun WaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onIme: (() -> Unit)? = null,
    enabled: Boolean = true,
    placeholder: String? = null,
    mono: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    autoCorrect: Boolean = false,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    val c = Wa.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, style = WaType.base.copy(fontSize = 13.5.sp), color = c.text2)
        val shape = RoundedCornerShape(8.dp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = WaType.base.copy(color = c.text).let { if (mono) it.merge(WaType.mono) else it },
            cursorBrush = SolidColor(c.accent),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
                autoCorrectEnabled = autoCorrect && !password,
                capitalization = capitalization,
            ),
            keyboardActions = KeyboardActions(onAny = { onIme?.invoke() }),
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.6f)
                .background(c.inputBg, shape)
                .border(BorderStroke(1.dp, if (focused) c.muted else c.border), shape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && placeholder != null) Text(placeholder, style = WaType.base, color = c.muted, maxLines = 1)
                    inner()
                }
            },
        )
    }
}

/** `.search-box`: gray rounded field with a search icon. */
@Composable
fun SearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Wa.colors
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(c.panel2).padding(start = 12.dp, end = 4.dp).heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WaIcon(WaIcons.search, 16.dp, c.muted)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = WaType.base.copy(color = c.text),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit?.invoke() }),
            modifier = Modifier.weight(1f).padding(vertical = 8.dp).let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = WaType.base, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) IconBtn(WaIcons.close, "Clear", { onValueChange("") }, size = 28.dp, iconSize = 16.dp)
        if (trailing != null) trailing()
    }
}

@Composable
fun CheckRow(checked: Boolean, onChange: (Boolean) -> Unit, label: String, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(enabled = enabled, role = Role.Checkbox) { onChange(!checked) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, onCheckedChange = null, enabled = enabled, colors = CheckboxDefaults.colors(checkedColor = Wa.colors.accent, checkmarkColor = Color.White, uncheckedColor = Wa.colors.muted), modifier = Modifier.padding(8.dp))
        Text(label, style = WaType.base, color = Wa.colors.text)
    }
}

@Composable
fun SwitchRow(checked: Boolean, onChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(
        modifier.clip(RoundedCornerShape(6.dp)).clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }.padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Wa.colors.accent, checkedThumbColor = Color.White, uncheckedTrackColor = Wa.colors.panel2, uncheckedBorderColor = Wa.colors.muted, uncheckedThumbColor = Wa.colors.muted),
        )
        Text(label, style = WaType.base, color = Wa.colors.text)
    }
}

/** `.modal`: centered dialog with a title row and a close button. */
@Composable
fun WaDialog(title: String, onClose: () -> Unit, wide: Boolean = false, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val c = Wa.colors
        Column(
            Modifier
                .padding(16.dp)
                .widthIn(max = if (wide) 1000.dp else 520.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.sheet),
        ) {
            // Instagram's dialog header: the title centered, close on the right.
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                Text(title, style = WaType.base.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = c.text, modifier = Modifier.padding(horizontal = 48.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconBtn(WaIcons.close, "Close", onClose, Modifier.align(Alignment.CenterEnd))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) { content() }
        }
    }
}

/** Settings `.settings-section`: bordered card with a title. */
@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, danger: Boolean = false, content: @Composable () -> Unit) {
    val c = Wa.colors
    Column(
        modifier.fillMaxWidth().widthIn(max = 900.dp).border(1.dp, if (danger) c.danger else c.border, RoundedCornerShape(12.dp)).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = WaType.sectionTitle, color = c.text)
        content()
    }
}

/** `.page-head`: bold title (Instagram shows no icon there), optional muted subtitle. */
@Composable
@Suppress("UNUSED_PARAMETER")
fun PageHeader(icon: ImageVector, title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = WaType.pageTitle, color = Wa.colors.text)
        if (subtitle != null) MutedText(subtitle, style = WaType.small)
    }
}

/** The app's name as a script wordmark (Instagram-style). */
@Composable
fun Wordmark(modifier: Modifier = Modifier, color: Color = Wa.colors.text, size: androidx.compose.ui.unit.TextUnit = 30.sp, text: String = "Instalogger") {
    Text(text, modifier, style = WaType.wordmark.copy(fontSize = size, lineHeight = size * 1.15f), color = color, maxLines = 1)
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Spacer(modifier.fillMaxWidth().height(1.dp).background(Wa.colors.border))
}

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))
