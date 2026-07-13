package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.effectImposedConditions
import au.com.evonet.nat20.dnd5e.core.Exhaustion
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The **Conditions** display strip (parity #19 slice C). Active conditions,
 * effect-imposed conditions (Greater Invisibility → Invisible), and an
 * exhaustion badge as read-only tinted chips. Applying / clearing conditions and
 * adjusting exhaustion moved into the Act sheet (Apply/Clear condition, which
 * routes exhaustion through AdjustExhaustion) — this strip is view-only, keeping
 * conditions visible on the sheet the way the effects strip is.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConditionsDisplayStrip(payload: DnD5ePayload) {
    val palette = MaterialTheme.natPalette
    val imposed = payload.effectImposedConditions
        .filterNot { c -> payload.activeConditions.any { it.equals(c, ignoreCase = true) } }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        payload.activeConditions.forEach { name -> ConditionDisplayChip(name, palette.danger) }
        imposed.forEach { name -> ConditionDisplayChip(name, palette.inkMute) }
        if (payload.exhaustionLevel > 0) {
            ConditionDisplayChip("Exhaustion · ${payload.exhaustionLevel}/${Exhaustion.MAX}", palette.danger)
        }
    }
}

/** True when the character has any condition or exhaustion worth showing. */
internal fun DnD5ePayload.hasVisibleConditions(): Boolean =
    activeConditions.isNotEmpty() || exhaustionLevel > 0 ||
        effectImposedConditions.any { c -> activeConditions.none { it.equals(c, ignoreCase = true) } }

@Composable
private fun ConditionDisplayChip(name: String, tone: Color) {
    val shape = RoundedCornerShape(3.dp)
    Row(
        Modifier
            .clip(shape)
            .border(1.dp, tone.copy(alpha = 0.45f), shape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = tone)
    }
}
