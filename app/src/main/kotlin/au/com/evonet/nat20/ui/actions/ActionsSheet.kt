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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.availableResourcePools
import au.com.evonet.nat20.dnd5e.castableSpellIDs
import au.com.evonet.nat20.dnd5e.isSpellcaster
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The in-campaign Actions sheet (parity #19, port of iOS `ActionsSheetView` +
 * `ActionCatalogue`): a parchment modal bottom sheet of grouped 2-column
 * action tiles under fading accent rule headers. Selection is reported by
 * tile id; `ActionsLayer.kt` maps ids onto pickers / existing flows.
 */

// ── Catalogue ──────────────────────────────────────────────────────────────────

internal enum class ActionIcon { COMBAT, REST, MAGIC, ITEM, QUEST, TRAVEL, BUFF, DEBUFF, CONCENTRATION }

internal data class ActionItem(
    val id: String,
    val label: String,
    val icon: ActionIcon,
    val danger: Boolean = false,
)

internal data class ActionGroup(val title: String, val items: List<ActionItem>)

/**
 * The 9-group catalogue, filtered for this character. Non-casters lose the
 * Magic group; conditional tiles (summons, Bind companion, Spend resource)
 * surface only when the relevant content is on the character.
 *
 * Slice-A gaps (deliberate — tiles omitted until their pickers land):
 * TODO(#19 slice B): "Learn spell" (wizard scroll-copy picker + intent audit).
 * TODO(#19 slice C): Effects group buff/debuff × ability/save/skill tiles,
 *   "Ally's spell", and "Custom effect" (EffectPicker / CustomEffectPicker /
 *   AllyCastPicker). Managing/cancelling effects stays reachable on the
 *   Combat tab, where the "Manage effects" and "Concentration" tiles route.
 * TODO(#19): "Recharge at dawn" — no RechargeAtDawn intent on Android yet
 *   (wondrous-item charge meters are a payload gap, see PARITY #17).
 */
internal fun actionGroups(payload: DnD5ePayload): List<ActionGroup> {
    val castable = payload.castableSpellIDs
    val canSpendResource = payload.availableResourcePools().isNotEmpty()
    val canBindCompanion = payload.classes.any {
        it.classId.equals("ranger", ignoreCase = true) && it.level >= 3 &&
            it.subclass?.contains("beast", ignoreCase = true) == true
    }

    return buildList {
        add(
            ActionGroup(
                "Session",
                listOf(
                    ActionItem("note", "Add journal entry", ActionIcon.QUEST),
                    ActionItem("level", "Level up", ActionIcon.MAGIC),
                    ActionItem("begin-encounter", "Begin encounter", ActionIcon.COMBAT),
                    ActionItem("encounter", "End encounter", ActionIcon.TRAVEL),
                ),
            ),
        )
        add(
            ActionGroup(
                "Vitals",
                listOf(
                    ActionItem("damage", "Take damage", ActionIcon.COMBAT, danger = true),
                    ActionItem("heal", "Heal", ActionIcon.REST),
                    ActionItem("temp", "Gain temp HP", ActionIcon.MAGIC),
                    ActionItem("death", "Mark death save", ActionIcon.COMBAT),
                ),
            ),
        )
        add(
            ActionGroup(
                "Roll",
                listOf(
                    ActionItem("attack", "Attack roll", ActionIcon.COMBAT),
                    ActionItem("skill", "Skill check", ActionIcon.QUEST),
                    ActionItem("save", "Saving throw", ActionIcon.QUEST),
                    ActionItem("check", "Ability check", ActionIcon.QUEST),
                ),
            ),
        )
        if (payload.isSpellcaster) {
            add(
                ActionGroup(
                    "Magic",
                    buildList {
                        add(ActionItem("cast", "Cast a spell", ActionIcon.MAGIC))
                        add(ActionItem("slot", "Expend a slot", ActionIcon.MAGIC))
                        if ("find-familiar" in castable) add(ActionItem("summon-familiar", "Summon familiar", ActionIcon.MAGIC))
                        if ("conjure-animals" in castable) add(ActionItem("conjure-animals", "Conjure animals", ActionIcon.MAGIC))
                        if (castable.any { it.startsWith("summon-") }) add(ActionItem("tashas-summon", "Summon spirit", ActionIcon.MAGIC))
                        if ("animate-dead" in castable) add(ActionItem("animate-dead", "Animate dead", ActionIcon.MAGIC))
                        if ("find-steed" in castable) add(ActionItem("find-steed", "Find steed", ActionIcon.MAGIC))
                    },
                ),
            )
        }
        add(
            ActionGroup(
                "Resources",
                buildList {
                    add(ActionItem("feature", "Use class feature", ActionIcon.MAGIC))
                    if (canSpendResource) add(ActionItem("spend-resource", "Spend resource", ActionIcon.MAGIC))
                    if (canBindCompanion) add(ActionItem("bind-companion", "Bind companion", ActionIcon.QUEST))
                    add(ActionItem("insp", "Spend inspiration", ActionIcon.QUEST))
                },
            ),
        )
        add(
            ActionGroup(
                "Effects",
                listOf(
                    ActionItem("manage-effects", "Manage effects", ActionIcon.MAGIC),
                    ActionItem("concentrate", "Concentration", ActionIcon.CONCENTRATION),
                ),
            ),
        )
        add(
            ActionGroup(
                "Conditions",
                listOf(
                    ActionItem("apply", "Apply condition", ActionIcon.COMBAT),
                    ActionItem("clear", "Clear condition", ActionIcon.REST),
                ),
            ),
        )
        add(
            ActionGroup(
                "Rest",
                listOf(
                    ActionItem("short-rest", "Short rest", ActionIcon.REST),
                    ActionItem("long-rest", "Long rest", ActionIcon.REST),
                    ActionItem("hit-die", "Spend hit die", ActionIcon.REST),
                ),
            ),
        )
        add(
            ActionGroup(
                "World",
                listOf(
                    ActionItem("add-item", "Acquire item", ActionIcon.ITEM),
                    ActionItem("use-item", "Use item", ActionIcon.ITEM),
                    ActionItem("drop", "Drop item", ActionIcon.ITEM),
                    ActionItem("coin", "Adjust coin", ActionIcon.ITEM),
                ),
            ),
        )
    }
}

// ── Sheet ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActionsSheetView(
    payload: DnD5ePayload,
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
            // Header: title + 30dp X circle.
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
                actionGroups(payload).forEach { group ->
                    GroupSection(group, onSelect)
                }
            }
        }
    }
}

@Composable
private fun GroupSection(group: ActionGroup, onSelect: (String) -> Unit) {
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
                    ActionTile(item, Modifier.weight(1f).fillMaxHeight()) { onSelect(item.id) }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActionTile(item: ActionItem, modifier: Modifier, onClick: () -> Unit) {
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
        IconDisc(item.icon, tint)
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
private fun IconDisc(icon: ActionIcon, tint: Color) {
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.067f))
            .border(1.dp, tint.copy(alpha = 0.33f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        ActionGlyph(icon, tint)
    }
}

/** Hand-drawn Canvas glyphs standing in for the iOS SF Symbols (project pattern). */
@Composable
private fun ActionGlyph(icon: ActionIcon, tint: Color) {
    Canvas(Modifier.size(13.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(1.2.dp.toPx())
        when (icon) {
            ActionIcon.COMBAT -> {
                // Shield: flat shoulders tapering to a point.
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
                // Crescent moon.
                val path = Path().apply {
                    moveTo(w * 0.68f, h * 0.06f)
                    cubicTo(w * 0.2f, h * 0.16f, w * 0.2f, h * 0.84f, w * 0.68f, h * 0.94f)
                    cubicTo(w * 0.38f, h * 0.8f, w * 0.38f, h * 0.2f, w * 0.68f, h * 0.06f)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
            ActionIcon.MAGIC -> {
                // Four-point spark.
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
                // Satchel: body + handle arc.
                drawRoundRect(
                    tint,
                    topLeft = Offset(w * 0.12f, h * 0.34f),
                    size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.58f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
                    style = stroke,
                )
                drawArc(
                    tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.3f, h * 0.08f),
                    size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.5f),
                    style = stroke,
                )
            }
            ActionIcon.QUEST -> {
                // Scroll: page with rolled top and bottom.
                drawLine(tint, Offset(w * 0.2f, h * 0.14f), Offset(w * 0.86f, h * 0.14f), stroke.width)
                drawLine(tint, Offset(w * 0.2f, h * 0.14f), Offset(w * 0.2f, h * 0.86f), stroke.width)
                drawLine(tint, Offset(w * 0.86f, h * 0.14f), Offset(w * 0.86f, h * 0.86f), stroke.width)
                drawLine(tint, Offset(w * 0.2f, h * 0.86f), Offset(w * 0.86f, h * 0.86f), stroke.width)
                drawLine(tint, Offset(w * 0.34f, h * 0.4f), Offset(w * 0.72f, h * 0.4f), stroke.width)
                drawLine(tint, Offset(w * 0.34f, h * 0.62f), Offset(w * 0.72f, h * 0.62f), stroke.width)
            }
            ActionIcon.TRAVEL -> {
                // Waymark arrow.
                drawLine(tint, Offset(w * 0.08f, h * 0.5f), Offset(w * 0.86f, h * 0.5f), stroke.width)
                drawLine(tint, Offset(w * 0.58f, h * 0.2f), Offset(w * 0.92f, h * 0.5f), stroke.width)
                drawLine(tint, Offset(w * 0.58f, h * 0.8f), Offset(w * 0.92f, h * 0.5f), stroke.width)
            }
            ActionIcon.BUFF -> {
                drawCircle(tint, radius = w * 0.46f, style = stroke)
                drawLine(tint, Offset(w * 0.3f, h * 0.56f), Offset(w * 0.5f, h * 0.32f), stroke.width)
                drawLine(tint, Offset(w * 0.7f, h * 0.56f), Offset(w * 0.5f, h * 0.32f), stroke.width)
            }
            ActionIcon.DEBUFF -> {
                drawCircle(tint, radius = w * 0.46f, style = stroke)
                drawLine(tint, Offset(w * 0.3f, h * 0.44f), Offset(w * 0.5f, h * 0.68f), stroke.width)
                drawLine(tint, Offset(w * 0.7f, h * 0.44f), Offset(w * 0.5f, h * 0.68f), stroke.width)
            }
            ActionIcon.CONCENTRATION -> {
                drawCircle(
                    tint,
                    radius = w * 0.46f,
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5f, 2.5f)),
                    ),
                )
            }
        }
    }
}
