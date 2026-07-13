package au.com.evonet.nat20.ui.actions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.pf2e.PathfinderPayload
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The in-campaign Pathfinder 2e Actions sheet (parity #35, port of iOS
 * `PathfinderActionsView`): a parchment modal bottom sheet of grouped 2-column
 * action tiles, opened by the codex Act pill. Every mutation the old 6-tab
 * interactive sheet fired inline now lives here (the #34 rebuild made the scroll
 * read-only), so nothing that worked before became unreachable.
 *
 * Groups mirror the iOS layout plus a **Manage** group Android needs but iOS
 * doesn't: iOS relocates equip/rune/learn/feat mutations into its edit flow, and
 * Android has no PF2e editor reopen yet, so they fold in here (noted in PARITY).
 * Selection is reported by tile id; [PathfinderActionsLayer] maps ids onto the
 * `Pf*` pickers / existing flows.
 */
internal object PfActionIds {
    // Vitals
    const val DAMAGE = "damage"
    const val HEAL = "heal"
    const val TEMP_HP = "tempHP"
    const val DYING_UP = "dyingUp"
    const val DYING_DOWN = "dyingDown"
    const val WOUNDED_UP = "woundedUp"
    const val WOUNDED_DOWN = "woundedDown"

    // Hero & resources
    const val HERO_SPEND = "heroSpend"
    const val HERO_GAIN = "heroGain"
    const val CAST_FOCUS = "castFocus"
    const val REFOCUS = "refocus"
    const val DAILY_PREP = "dailyPrep"

    // Combat
    const val STRIKE = "strike"
    const val CAST = "cast"

    // Conditions
    const val APPLY_CONDITION = "applyCondition"
    const val CLEAR_CONDITION = "clearCondition"

    // Inventory
    const val ADD_ITEM = "addItem"
    const val ADJUST_COIN = "adjustCoin"

    // Manage (Android-only edit-flow fold)
    const val EQUIP_ARMOR = "equipArmor"
    const val EQUIP_SHIELD = "equipShield"
    const val RAISE_SHIELD = "raiseShield"
    const val ADD_WEAPON = "addWeapon"
    const val REMOVE_WEAPON = "removeWeapon"
    const val ETCH_WEAPON_RUNES = "etchWeaponRunes"
    const val ETCH_ARMOR_RUNES = "etchArmorRunes"
    const val LEARN_SPELL = "learnSpell"
    const val TAKE_FEAT = "takeFeat"
    const val REMOVE_FEAT = "removeFeat"
    const val ADD_NOTE = "addNote"

    // Progress
    const val LEVEL_UP = "levelUp"
}

/** The PF2e action groups, filtered for this character (mirrors iOS `PathfinderActionsView.groups`). */
internal fun pathfinderActionGroups(
    payload: PathfinderPayload,
    canLevelUp: Boolean,
): List<ActionGroup> = buildList {
    add(
        ActionGroup(
            "Vitals",
            buildList {
                add(ActionItem(PfActionIds.DAMAGE, "Take Damage", ActionIcon.COMBAT, danger = true))
                add(ActionItem(PfActionIds.HEAL, "Heal", ActionIcon.REST))
                add(ActionItem(PfActionIds.TEMP_HP, "Gain Temp HP", ActionIcon.MAGIC))
                add(ActionItem(PfActionIds.DYING_UP, "Dying +1", ActionIcon.COMBAT, danger = true))
                if (payload.dying > 0) add(ActionItem(PfActionIds.DYING_DOWN, "Dying −1", ActionIcon.REST))
                add(ActionItem(PfActionIds.WOUNDED_UP, "Wounded +1", ActionIcon.COMBAT, danger = true))
                if (payload.wounded > 0) add(ActionItem(PfActionIds.WOUNDED_DOWN, "Wounded −1", ActionIcon.REST))
            },
        ),
    )

    add(
        ActionGroup(
            "Hero & Resources",
            buildList {
                if (payload.heroPoints > 0) add(ActionItem(PfActionIds.HERO_SPEND, "Spend Hero Point", ActionIcon.QUEST))
                if (payload.heroPoints < PathfinderPayload.HERO_POINT_MAX) add(ActionItem(PfActionIds.HERO_GAIN, "Gain Hero Point", ActionIcon.QUEST))
                if (payload.maxFocusPoints > 0) {
                    if (payload.focusPoints > 0 && payload.focusSpells.isNotEmpty()) add(ActionItem(PfActionIds.CAST_FOCUS, "Cast Focus Spell", ActionIcon.MAGIC))
                    if (payload.focusPoints < payload.maxFocusPoints) add(ActionItem(PfActionIds.REFOCUS, "Refocus", ActionIcon.REST))
                }
                add(ActionItem(PfActionIds.DAILY_PREP, "Daily Preparations", ActionIcon.REST))
            },
        ),
    )

    val combat = buildList {
        if (payload.weapons.isNotEmpty()) add(ActionItem(PfActionIds.STRIKE, "Strike", ActionIcon.COMBAT))
        if (payload.spellTradition != null) add(ActionItem(PfActionIds.CAST, "Cast Spell", ActionIcon.MAGIC))
    }
    if (combat.isNotEmpty()) add(ActionGroup("Combat", combat))

    add(
        ActionGroup(
            "Conditions",
            buildList {
                add(ActionItem(PfActionIds.APPLY_CONDITION, "Apply Condition", ActionIcon.DEBUFF))
                if (payload.conditions.isNotEmpty()) add(ActionItem(PfActionIds.CLEAR_CONDITION, "Clear Condition", ActionIcon.REST))
            },
        ),
    )

    add(
        ActionGroup(
            "Inventory",
            listOf(
                ActionItem(PfActionIds.ADD_ITEM, "Add Item", ActionIcon.ITEM),
                ActionItem(PfActionIds.ADJUST_COIN, "Adjust Coin", ActionIcon.ITEM),
            ),
        ),
    )

    add(
        ActionGroup(
            "Manage",
            buildList {
                add(ActionItem(PfActionIds.EQUIP_ARMOR, "Equip Armor", ActionIcon.ITEM))
                add(ActionItem(PfActionIds.EQUIP_SHIELD, "Equip Shield", ActionIcon.ITEM))
                if (payload.shield != null) add(ActionItem(PfActionIds.RAISE_SHIELD, if (payload.shieldRaised) "Lower Shield" else "Raise Shield", ActionIcon.BUFF))
                add(ActionItem(PfActionIds.ADD_WEAPON, "Add Weapon", ActionIcon.COMBAT))
                if (payload.weapons.isNotEmpty()) add(ActionItem(PfActionIds.REMOVE_WEAPON, "Remove Weapon", ActionIcon.COMBAT, danger = true))
                if (payload.weapons.isNotEmpty()) add(ActionItem(PfActionIds.ETCH_WEAPON_RUNES, "Etch Weapon Runes", ActionIcon.MAGIC))
                if (payload.armor != null) add(ActionItem(PfActionIds.ETCH_ARMOR_RUNES, "Etch Armor Runes", ActionIcon.MAGIC))
                if (payload.spellTradition != null) add(ActionItem(PfActionIds.LEARN_SPELL, "Learn Spell", ActionIcon.MAGIC))
                add(ActionItem(PfActionIds.TAKE_FEAT, "Take Feat", ActionIcon.QUEST))
                if (payload.feats.isNotEmpty()) add(ActionItem(PfActionIds.REMOVE_FEAT, "Remove Feat", ActionIcon.QUEST, danger = true))
                add(ActionItem(PfActionIds.ADD_NOTE, "Add Note", ActionIcon.QUEST))
            },
        ),
    )

    if (canLevelUp) add(ActionGroup("Progress", listOf(ActionItem(PfActionIds.LEVEL_UP, "Level Up", ActionIcon.MAGIC))))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PathfinderActionsSheet(
    payload: PathfinderPayload,
    canLevelUp: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.parchment,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(palette.ink.copy(alpha = 0.35f)),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.82f)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Choose an action",
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = palette.ink,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(palette.tileStrong)
                        .border(1.dp, palette.ink.copy(alpha = 0.53f), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 11.sp, color = palette.ink)
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pathfinderActionGroups(payload, canLevelUp).forEach { group -> PfGroupSection(group, onSelect) }
            }
        }
    }
}

// ── Rendering (mirrors Actions2024Sheet's private tile/group composables). ──

@Composable
private fun PfGroupSection(group: ActionGroup, onSelect: (String) -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                group.title.uppercase(),
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 3.5.sp,
                color = palette.accent,
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(palette.accent.copy(alpha = 0.4f), Color.Transparent))),
            )
        }
        group.items.chunked(2).forEach { rowItems ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowItems.forEach { item ->
                    PfActionTile(item, Modifier.weight(1f).fillMaxHeight()) { onSelect(item.id) }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PfActionTile(item: ActionItem, modifier: Modifier, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val tint = if (item.danger) palette.danger else palette.ink
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier
            .clip(shape)
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.2f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PfIconDisc(item.icon, tint)
        Text(item.label, fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = tint, maxLines = 2)
    }
}

@Composable
private fun PfIconDisc(icon: ActionIcon, tint: Color) {
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.067f))
            .border(1.dp, tint.copy(alpha = 0.33f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(13.dp)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(1.2.dp.toPx())
            when (icon) {
                ActionIcon.COMBAT -> {
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.04f); lineTo(w * 0.92f, h * 0.2f); lineTo(w * 0.86f, h * 0.6f)
                        lineTo(w * 0.5f, h * 0.96f); lineTo(w * 0.14f, h * 0.6f); lineTo(w * 0.08f, h * 0.2f); close()
                    }
                    drawPath(path, tint, style = stroke)
                }
                ActionIcon.REST -> {
                    val path = Path().apply {
                        moveTo(w * 0.68f, h * 0.06f)
                        cubicTo(w * 0.2f, h * 0.16f, w * 0.2f, h * 0.84f, w * 0.68f, h * 0.94f)
                        cubicTo(w * 0.38f, h * 0.8f, w * 0.38f, h * 0.2f, w * 0.68f, h * 0.06f); close()
                    }
                    drawPath(path, tint, style = stroke)
                }
                ActionIcon.MAGIC, ActionIcon.BUFF -> {
                    val path = Path().apply {
                        moveTo(w * 0.5f, 0f)
                        quadraticBezierTo(w * 0.56f, h * 0.44f, w, h * 0.5f)
                        quadraticBezierTo(w * 0.56f, h * 0.56f, w * 0.5f, h)
                        quadraticBezierTo(w * 0.44f, h * 0.56f, 0f, h * 0.5f)
                        quadraticBezierTo(w * 0.44f, h * 0.44f, w * 0.5f, 0f); close()
                    }
                    drawPath(path, tint)
                }
                ActionIcon.ITEM -> {
                    drawRoundRect(
                        tint,
                        topLeft = Offset(w * 0.12f, h * 0.34f),
                        size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.58f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
                        style = stroke,
                    )
                    drawArc(
                        tint, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                        topLeft = Offset(w * 0.3f, h * 0.08f),
                        size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.5f), style = stroke,
                    )
                }
                else -> {
                    drawLine(tint, Offset(w * 0.2f, h * 0.14f), Offset(w * 0.86f, h * 0.14f), stroke.width)
                    drawLine(tint, Offset(w * 0.2f, h * 0.14f), Offset(w * 0.2f, h * 0.86f), stroke.width)
                    drawLine(tint, Offset(w * 0.86f, h * 0.14f), Offset(w * 0.86f, h * 0.86f), stroke.width)
                    drawLine(tint, Offset(w * 0.2f, h * 0.86f), Offset(w * 0.86f, h * 0.86f), stroke.width)
                    drawLine(tint, Offset(w * 0.34f, h * 0.4f), Offset(w * 0.72f, h * 0.4f), stroke.width)
                    drawLine(tint, Offset(w * 0.34f, h * 0.62f), Offset(w * 0.72f, h * 0.62f), stroke.width)
                }
            }
        }
    }
}
