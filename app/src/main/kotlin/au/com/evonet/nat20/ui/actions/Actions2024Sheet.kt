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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The in-campaign 2024 Actions sheet (parity #31, port of iOS
 * `Actions2024SheetView`): a parchment modal bottom sheet of grouped 2-column
 * action tiles, opened by the codex Act pill. It is deliberately **simpler**
 * than the 2014 [ActionsSheetView] — iOS ships the 2024 sheet with five groups
 * (Combat / Vitals / Inventory / Progress / Conditions), no skill/save/ability
 * checks, no summons, no metamagic/pact/invocation, no effects group, and no
 * begin/end-encounter, hit-die, or death-save tiles. Selection is reported by
 * tile id; [Actions2024Layer] maps ids onto the 2024 pickers / existing flows.
 *
 * Two iOS 2024 tiles are omitted here (module gap, not a design choice): **Spend
 * Resource** and **Short Rest** — the Android `:ruleset-dnd5e-2024` module never
 * ported the class-resource-pool accessor / `SpendResource2024` intent nor a
 * `ShortRest2024` intent, so there is nothing to fire. Adding them belongs to a
 * domain slice, not this UI layer.
 */

// The tile ids mirror the iOS `Action2024` raw values so the dispatch reads 1:1.
internal object Action2024Ids {
    const val ATTACK = "attack"
    const val CAST = "cast"
    const val DAMAGE = "damage"
    const val HEAL = "heal"
    const val TEMP_HP = "tempHP"
    const val ADD_ITEM = "addItem"
    const val ADD_GOLD = "addGold"
    const val SPEND_GOLD = "spendGold"
    const val LEVEL_UP = "levelUp"
    const val LONG_REST = "longRest"
    const val INSPIRATION = "inspiration"
    const val EXHAUSTION_UP = "exhaustionUp"
    const val EXHAUSTION_DOWN = "exhaustionDown"
    const val APPLY_CONDITION = "applyCondition"
    const val CLEAR_CONDITION = "clearCondition"
}

/**
 * The five 2024 groups, filtered for this character (parity with iOS
 * `Actions2024SheetView.groups`): Combat drops the Attack/Cast tiles unless the
 * character can use them; Progress shows Level Up only below 20; Conditions
 * shows Clear only when something is active.
 */
internal fun actionGroups2024(
    canAttack: Boolean,
    canCast: Boolean,
    canLevelUp: Boolean,
    hasActiveConditions: Boolean,
): List<ActionGroup> = buildList {
    val combat = buildList {
        if (canAttack) add(ActionItem(Action2024Ids.ATTACK, "Attack", ActionIcon.COMBAT))
        if (canCast) add(ActionItem(Action2024Ids.CAST, "Cast Spell", ActionIcon.MAGIC))
    }
    if (combat.isNotEmpty()) add(ActionGroup("Combat", combat))

    add(
        ActionGroup(
            "Vitals",
            listOf(
                ActionItem(Action2024Ids.DAMAGE, "Take Damage", ActionIcon.COMBAT, danger = true),
                ActionItem(Action2024Ids.HEAL, "Heal", ActionIcon.REST),
                ActionItem(Action2024Ids.TEMP_HP, "Gain Temp HP", ActionIcon.MAGIC),
            ),
        ),
    )

    add(
        ActionGroup(
            "Inventory",
            listOf(
                ActionItem(Action2024Ids.ADD_ITEM, "Add Item", ActionIcon.ITEM),
                ActionItem(Action2024Ids.ADD_GOLD, "Add Gold", ActionIcon.ITEM),
                ActionItem(Action2024Ids.SPEND_GOLD, "Spend Gold", ActionIcon.ITEM, danger = true),
            ),
        ),
    )

    add(
        ActionGroup(
            "Progress",
            buildList {
                if (canLevelUp) add(ActionItem(Action2024Ids.LEVEL_UP, "Level Up", ActionIcon.MAGIC))
                add(ActionItem(Action2024Ids.LONG_REST, "Long Rest", ActionIcon.REST))
                add(ActionItem(Action2024Ids.INSPIRATION, "Heroic Inspiration", ActionIcon.QUEST))
            },
        ),
    )

    add(
        ActionGroup(
            "Conditions",
            buildList {
                add(ActionItem(Action2024Ids.EXHAUSTION_UP, "Exhaustion +1", ActionIcon.REST))
                add(ActionItem(Action2024Ids.EXHAUSTION_DOWN, "Exhaustion −1", ActionIcon.REST))
                add(ActionItem(Action2024Ids.APPLY_CONDITION, "Apply Condition", ActionIcon.COMBAT))
                if (hasActiveConditions) add(ActionItem(Action2024Ids.CLEAR_CONDITION, "Clear Condition", ActionIcon.REST))
            },
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Actions2024SheetView(
    payload: DnD5e2024Payload,
    canAttack: Boolean,
    canCast: Boolean,
    canLevelUp: Boolean,
    hasActiveConditions: Boolean,
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
                actionGroups2024(canAttack, canCast, canLevelUp, hasActiveConditions).forEach { group ->
                    GroupSection2024(group, onSelect)
                }
            }
        }
    }
}

// ── Rendering (mirrors ActionsSheet.kt's private tile/group composables, which
//    can't be imported across files; kept byte-identical for a matching look). ──

@Composable
private fun GroupSection2024(group: ActionGroup, onSelect: (String) -> Unit) {
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
                    .background(
                        Brush.horizontalGradient(
                            listOf(palette.accent.copy(alpha = 0.4f), Color.Transparent),
                        ),
                    ),
            )
        }
        group.items.chunked(2).forEach { rowItems ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowItems.forEach { item ->
                    ActionTile2024(item, Modifier.weight(1f).fillMaxHeight()) { onSelect(item.id) }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActionTile2024(item: ActionItem, modifier: Modifier, onClick: () -> Unit) {
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
        IconDisc2024(item.icon, tint)
        Text(
            item.label,
            fontFamily = Cormorant,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = tint,
            maxLines = 2,
        )
    }
}

@Composable
private fun IconDisc2024(icon: ActionIcon, tint: Color) {
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.067f))
            .border(1.dp, tint.copy(alpha = 0.33f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        ActionGlyph2024(icon, tint)
    }
}

/** Hand-drawn Canvas glyphs standing in for the iOS SF Symbols (project pattern). */
@Composable
private fun ActionGlyph2024(icon: ActionIcon, tint: Color) {
    Canvas(Modifier.size(13.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(1.2.dp.toPx())
        when (icon) {
            ActionIcon.COMBAT -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.04f)
                    lineTo(w * 0.92f, h * 0.2f)
                    lineTo(w * 0.86f, h * 0.6f)
                    lineTo(w * 0.5f, h * 0.96f)
                    lineTo(w * 0.14f, h * 0.6f)
                    lineTo(w * 0.08f, h * 0.2f)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
            ActionIcon.REST -> {
                val path = Path().apply {
                    moveTo(w * 0.68f, h * 0.06f)
                    cubicTo(w * 0.2f, h * 0.16f, w * 0.2f, h * 0.84f, w * 0.68f, h * 0.94f)
                    cubicTo(w * 0.38f, h * 0.8f, w * 0.38f, h * 0.2f, w * 0.68f, h * 0.06f)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
            ActionIcon.MAGIC -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    quadraticBezierTo(w * 0.56f, h * 0.44f, w, h * 0.5f)
                    quadraticBezierTo(w * 0.56f, h * 0.56f, w * 0.5f, h)
                    quadraticBezierTo(w * 0.44f, h * 0.56f, 0f, h * 0.5f)
                    quadraticBezierTo(w * 0.44f, h * 0.44f, w * 0.5f, 0f)
                    close()
                }
                drawPath(path, tint)
            }
            ActionIcon.ITEM -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(w * 0.12f, h * 0.34f),
                    size = Size(w * 0.76f, h * 0.58f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
                    style = stroke,
                )
                drawArc(
                    tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.3f, h * 0.08f),
                    size = Size(w * 0.4f, h * 0.5f),
                    style = stroke,
                )
            }
            ActionIcon.QUEST -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.14f), Offset(w * 0.86f, h * 0.14f), stroke.width)
                drawLine(tint, Offset(w * 0.2f, h * 0.14f), Offset(w * 0.2f, h * 0.86f), stroke.width)
                drawLine(tint, Offset(w * 0.86f, h * 0.14f), Offset(w * 0.86f, h * 0.86f), stroke.width)
                drawLine(tint, Offset(w * 0.2f, h * 0.86f), Offset(w * 0.86f, h * 0.86f), stroke.width)
                drawLine(tint, Offset(w * 0.34f, h * 0.4f), Offset(w * 0.72f, h * 0.4f), stroke.width)
                drawLine(tint, Offset(w * 0.34f, h * 0.62f), Offset(w * 0.72f, h * 0.62f), stroke.width)
            }
            else -> {
                // Only COMBAT / REST / MAGIC / ITEM / QUEST are used by the 2024 sheet;
                // fall back to a simple ring for any other icon.
                drawCircle(tint, radius = w * 0.46f, style = stroke)
            }
        }
    }
}
