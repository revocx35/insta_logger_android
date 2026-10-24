package io.github.revocx35.instalogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.revocx35.instalogger.data.Account
import io.github.revocx35.instalogger.ui.components.Badge
import io.github.revocx35.instalogger.ui.components.BadgeTone
import io.github.revocx35.instalogger.ui.components.BtnStyle
import io.github.revocx35.instalogger.ui.components.ConfirmButton
import io.github.revocx35.instalogger.ui.components.Divider
import io.github.revocx35.instalogger.ui.components.MutedText
import io.github.revocx35.instalogger.ui.components.WaButton
import io.github.revocx35.instalogger.ui.components.WaDialog
import io.github.revocx35.instalogger.ui.theme.Wa
import io.github.revocx35.instalogger.ui.theme.WaIcons
import io.github.revocx35.instalogger.ui.theme.WaType

/** The account's initial in a coloured circle (there is no avatar on the server for the app's own account). */
@Composable
fun AccountAvatar(account: Account, size: Dp = 40.dp) {
    val c = Wa.colors
    val initials = account.label.split(' ', '@', '_', '.', '-', '+')
        .filter { it.isNotBlank() }.take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
    Box(Modifier.size(size).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
        Text(initials.ifEmpty { "?" }, style = WaType.base.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}

/**
 * The account menu: everything added on this phone, with the open one marked. Tapping another account
 * switches to it without signing either out; "Add another account" opens the server screen.
 */
@Composable
fun AccountSwitcherDialog(
    accounts: List<Account>,
    active: Account?,
    onSwitch: (Account) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Account) -> Unit,
    onClose: () -> Unit,
) {
    val c = Wa.colors
    WaDialog("Accounts", onClose) {
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MutedText(
                "Each account is one server (or one user on it) with its own sign-in on this phone. Switching keeps the others signed in.",
                style = WaType.small,
            )
            Column {
                accounts.forEachIndexed { i, a ->
                    if (i > 0) Divider()
                    val current = a.id == active?.id
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (current) c.active else Color.Transparent)
                            .clickable(enabled = !current, role = Role.Button, onClickLabel = "Switch to ${a.label}") { onSwitch(a) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AccountAvatar(a)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(a.label, style = WaType.base.copy(fontWeight = FontWeight.SemiBold), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (current) Badge("current", BadgeTone.Green)
                            }
                            MutedText(a.address, style = WaType.tiny, maxLines = 1)
                            if (a.username == null) MutedText("not signed in — tap to sign in", style = WaType.tiny)
                        }
                        ConfirmButton(
                            if (current) "Sign out & remove" else "Remove",
                            "Confirm",
                            { onRemove(a) },
                            style = BtnStyle.Ghost,
                        )
                    }
                }
            }
            WaButton("Add another account", onAdd, style = BtnStyle.Primary, block = true, icon = WaIcons.plus)
            WaButton("Close", onClose, style = BtnStyle.Ghost, block = true)
        }
    }
}

/** The open account at the top of Settings: tap to switch, like tapping your profile. */
@Composable
fun AccountCard(account: Account?, username: String?, serverLabel: String, count: Int, onOpen: () -> Unit) {
    val c = Wa.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClickLabel = "Accounts") { onOpen() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (account != null) {
            AccountAvatar(account, 44.dp)
        } else {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.panel2), contentAlignment = Alignment.Center) {
                io.github.revocx35.instalogger.ui.components.WaIcon(WaIcons.user, 22.dp, c.muted)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(username ?: account?.label ?: "Not signed in", style = WaType.base.copy(fontWeight = FontWeight.SemiBold), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (count > 1) Badge("$count accounts", BadgeTone.Gray)
            }
            MutedText(account?.address ?: serverLabel, style = WaType.small, maxLines = 1)
        }
        Text(if (count > 1) "Switch" else "Add account", style = WaType.small.copy(fontWeight = FontWeight.SemiBold), color = c.link)
    }
}
