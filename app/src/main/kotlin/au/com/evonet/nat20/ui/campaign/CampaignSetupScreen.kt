package au.com.evonet.nat20.ui.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.domain.PartyMember
import au.com.evonet.nat20.ui.actions.ActionPickerShell
import au.com.evonet.nat20.ui.codex.SectionHead
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The shared campaign setup screen (parity #37), a port of the iOS
 * `CampaignSetupView`. Two modes on one layout:
 *  - **create**: a begin-notice, the name field, the party roster editor, and a
 *    "Begin Campaign" footer that hands the whole roster back via [onBegin].
 *  - **edit**: an inline-committing name field, the party editor (changes emit
 *    incrementally via [onPartyChange]), and a Danger section whose Leave action
 *    ends the campaign (Android has one owning character, so leave == end).
 *
 * Presented full-screen on the shared [ActionPickerShell] parchment chrome.
 */
@Composable
fun CampaignSetupScreen(
    create: Boolean,
    initialName: String = "",
    initialParty: List<PartyMember> = emptyList(),
    onCancel: () -> Unit,
    onBegin: (String, List<PartyMember>) -> Unit = { _, _ -> },
    onRename: (String) -> Unit = {},
    onPartyChange: (List<PartyMember>) -> Unit = {},
    onLeave: () -> Unit = {},
) {
    val palette = MaterialTheme.natPalette
    var name by remember { mutableStateOf(initialName) }
    var committedName by remember { mutableStateOf(initialName.trim()) }
    val party = remember { mutableStateListOf<PartyMember>().apply { addAll(initialParty) } }
    var editorFor by remember { mutableStateOf<PartyMember?>(null) }
    var addingMember by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }

    val trimmed = name.trim()
    val title = trimmed.ifEmpty { if (create) "New Campaign" else "Campaign" }

    fun emitParty() {
        if (!create) onPartyChange(party.toList())
    }

    ActionPickerShell(
        kicker = "Settings",
        title = title,
        onCancel = onCancel,
        footer = if (create) {
            {
                PrimaryActionButton(label = "Begin Campaign", isDisabled = trimmed.isEmpty()) {
                    onBegin(trimmed, party.toList())
                }
            }
        } else {
            null
        },
    ) {
        // ── Campaign ────────────────────────────────────────────────────────
        SectionHead("Campaign", top = 16.dp, bottom = 10.dp)
        if (create) {
            BeginNotice()
            Spacer(Modifier.size(12.dp))
        }
        FieldLabel("Name", required = true)
        Spacer(Modifier.size(6.dp))
        if (create) {
            WizardTextField("Untitled Campaign", name, { name = it })
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WizardTextField("Untitled Campaign", name, { name = it }, modifier = Modifier.weight(1f))
                val canRename = trimmed.isNotEmpty() && trimmed != committedName
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (canRename) palette.accent else palette.tileStrong)
                        .border(1.dp, palette.accent.copy(alpha = if (canRename) 1f else 0.5f), CircleShape)
                        .then(if (canRename) Modifier.clickable { onRename(trimmed); committedName = trimmed } else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Rename",
                        tint = if (canRename) palette.cream else palette.inkMute,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // ── Your Companions ─────────────────────────────────────────────────
        SectionHead("Your Companions", top = 22.dp, bottom = 10.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            party.forEach { member ->
                if (editorFor?.id == member.id) {
                    PartyEditorCard(
                        existing = member,
                        onCancel = { editorFor = null },
                        onSave = { updated ->
                            val idx = party.indexOfFirst { it.id == member.id }
                            if (idx >= 0) party[idx] = updated
                            editorFor = null
                            emitParty()
                        },
                    )
                } else {
                    PartyMemberRow(
                        member = member,
                        onEdit = { editorFor = member; addingMember = false },
                        onRemove = { party.remove(member); emitParty() },
                    )
                }
            }
            if (addingMember) {
                PartyEditorCard(
                    existing = null,
                    onCancel = { addingMember = false },
                    onSave = { added -> party.add(added); addingMember = false; emitParty() },
                )
            } else {
                AddMemberButton { addingMember = true; editorFor = null }
            }
        }

        // ── Danger (edit only) ──────────────────────────────────────────────
        if (!create) {
            SectionHead("Danger", top = 22.dp, bottom = 10.dp)
            if (confirmLeave) {
                LeaveConfirmCard(
                    campaignName = committedName.ifEmpty { "this campaign" },
                    onStay = { confirmLeave = false },
                    onLeave = onLeave,
                )
            } else {
                LeaveTile { confirmLeave = true }
            }
        }

        Spacer(Modifier.size(32.dp))
    }
}

// ── Field atoms (iOS CampaignSetupFieldLabel) ────────────────────────────────

@Composable
private fun FieldLabel(label: String, required: Boolean = false) {
    val palette = MaterialTheme.natPalette
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.5.sp,
            color = palette.inkMute,
        )
        if (required) {
            Text(" *", fontFamily = Cinzel, fontSize = 11.sp, color = palette.accent)
        }
    }
}

@Composable
private fun BeginNotice() {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.accent.copy(alpha = 0.06f))
            .border(1.dp, palette.accent.copy(alpha = 0.33f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Text(
            "Once your adventure begins, this character can no longer be edited — changes happen through play.",
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = palette.inkSoft,
        )
    }
}

// ── Party member row + editor ────────────────────────────────────────────────

@Composable
private fun PartyMemberRow(member: PartyMember, onEdit: () -> Unit, onRemove: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(palette.track).border(1.dp, palette.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials(member.name), fontFamily = Cinzel, fontSize = 12.sp, color = palette.accent)
        }
        Column(Modifier.weight(1f)) {
            Text(member.name, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = palette.ink, maxLines = 1)
            val meta = listOfNotNull(
                member.characterClass?.takeIf { it.isNotBlank() },
                member.race?.takeIf { it.isNotBlank() },
                member.level?.let { "L$it" },
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = palette.inkSoft, maxLines = 1)
            }
        }
        IconCircle(Icons.Filled.Edit, "Edit member", palette.ink, onEdit)
        IconCircle(Icons.Filled.Close, "Remove member", palette.danger, onRemove)
    }
}

@Composable
private fun IconCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(palette.tileStrong).border(1.dp, tint.copy(alpha = 0.4f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun PartyEditorCard(existing: PartyMember?, onCancel: () -> Unit, onSave: (PartyMember) -> Unit) {
    val palette = MaterialTheme.natPalette
    var mName by remember { mutableStateOf(existing?.name ?: "") }
    var mClass by remember { mutableStateOf(existing?.characterClass ?: "") }
    var mRace by remember { mutableStateOf(existing?.race ?: "") }
    var mLevel by remember { mutableStateOf(existing?.level?.toString() ?: "") }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tileStrong)
            .border(1.4.dp, palette.accent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (existing == null) "New Member" else "Edit Member",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.accent,
        )
        FieldLabel("Name", required = true)
        WizardTextField("Their name", mName, { mName = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel("Class")
                WizardTextField("e.g. Fighter", mClass, { mClass = it })
            }
            Column(Modifier.weight(1f)) {
                FieldLabel("Race")
                WizardTextField("e.g. Half-Elf", mRace, { mRace = it })
            }
        }
        Column(Modifier.width(96.dp)) {
            FieldLabel("Level")
            WizardTextField("1–20", mLevel, { new -> mLevel = new.filter { it.isDigit() }.take(2) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                PillButton("Cancel", filled = false, enabled = true, onClick = onCancel)
            }
            Box(Modifier.weight(1f)) {
                PillButton(if (existing == null) "Add" else "Save", filled = true, enabled = mName.isNotBlank()) {
                    onSave(
                        (existing ?: PartyMember(name = mName.trim())).copy(
                            name = mName.trim(),
                            characterClass = mClass.trim().ifBlank { null },
                            race = mRace.trim().ifBlank { null },
                            level = mLevel.toIntOrNull(),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMemberButton(onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val stroke = palette.accent.copy(alpha = 0.5f)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .drawBehind {
                drawRoundRect(
                    color = stroke,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(
                        width = 1.4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            }
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "+ ADD PARTY MEMBER",
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.5.sp,
            color = palette.accent,
        )
    }
}

// ── Danger ───────────────────────────────────────────────────────────────────

@Composable
private fun LeaveTile(onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.danger.copy(alpha = 0.06f))
            .border(1.dp, palette.danger.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("LEAVE CAMPAIGN", fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.danger)
            Text(
                "This closes the campaign and freezes its journal.",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = palette.inkSoft,
            )
        }
    }
}

@Composable
private fun LeaveConfirmCard(campaignName: String, onStay: () -> Unit, onLeave: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tileStrong)
            .border(1.4.dp, palette.danger.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Are you sure?", fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic, fontSize = 17.sp, color = palette.ink)
        Text(
            "This closes $campaignName and returns the character to building. Its journal stays in Past Adventures.",
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = palette.inkSoft,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { PillButton("Stay", filled = false, enabled = true, onClick = onStay) }
            Box(Modifier.weight(1f)) { PillButton("Leave Campaign", filled = true, enabled = true, danger = true, onClick = onLeave) }
        }
    }
}

// ── Shared pill ──────────────────────────────────────────────────────────────

@Composable
private fun PillButton(label: String, filled: Boolean, enabled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val base = if (danger) palette.danger else palette.accent
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .then(if (filled) Modifier.background(base) else Modifier)
            .border(1.dp, base.copy(alpha = if (filled) 1f else 0.4f), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontWeight = if (filled) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = if (filled) palette.cream else base,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

private fun initials(name: String): String =
    name.trim().split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
