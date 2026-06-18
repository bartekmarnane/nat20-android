package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.dnd5e.core.HpChoice
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Catalog
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.dnd5e2024.LevelUp2024
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.slugToTitle

private enum class HpMode2024 { AVERAGE, ROLL }
private enum class AsiKind2024 { ONE, TWO }

/**
 * The D&D 5e (2024) level-up wizard (A21): advance a class (or multiclass),
 * choose HP (average or rolled via the A16 primitive), pick a subclass at level
 * 3 (the 2024 rule for every class), and allocate an Ability Score Improvement
 * at ASI levels. Mirrors the 2014 level-up wizard over the 2024 catalogue.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LevelUp2024Wizard(payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit, onDismiss: () -> Unit) {
    val existing = payload.classes.map { it.classId }
    var classId by remember { mutableStateOf(payload.classes.firstOrNull()?.classId ?: "fighter") }
    val isNew = classId !in existing
    val newClassLevel = (payload.classes.firstOrNull { it.classId == classId }?.level ?: 0) + 1
    val klass = DnD5e2024Catalog.characterClass(classId)
    val hitDie = DnD5eClasses.hitDie(classId)
    val conMod = AbilityScores.modifier(payload.abilityScores.constitution)

    var hpMode by remember { mutableStateOf(HpMode2024.AVERAGE) }
    var rolledDie by remember(classId) { mutableStateOf<Int?>(null) }

    val needsSubclass = klass != null && newClassLevel == klass.subclassLevel &&
        payload.classes.firstOrNull { it.classId == classId }?.subclass == null && klass.subclasses.isNotEmpty()
    var subclass by remember(classId) { mutableStateOf<String?>(null) }

    val needsAsi = LevelUpMath.grantsAbilityScoreImprovement(classId, newClassLevel)
    var asiKind by remember { mutableStateOf(AsiKind2024.ONE) }
    var picks by remember(classId) { mutableStateOf<List<Ability>>(emptyList()) }
    val asi = when (asiKind) { AsiKind2024.ONE -> picks.take(1).associateWith { 2 }; AsiKind2024.TWO -> picks.take(2).associateWith { 1 } }

    val ready = klass != null && payload.level < DnD5e2024Payload.MAX_LEVEL &&
        (hpMode == HpMode2024.AVERAGE || rolledDie != null) &&
        (!needsSubclass || subclass != null) &&
        (!needsAsi || asi.values.sum() == 2)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Level Up · 2024") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Label2024("Class")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DnD5e2024Catalog.classes.forEach { c ->
                        val label = c.name + (payload.classes.firstOrNull { it.classId == c.id }?.let { " ${it.level}" } ?: " · new")
                        FilterChip(c.id == classId, { classId = c.id }, label = { Text(label) })
                    }
                }
                Text("→ ${klass?.name ?: classId.slugToTitle()} $newClassLevel" + if (isNew) " (multiclass)" else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                Label2024("Hit Points (d$hitDie, CON ${conMod.signed3()})")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(hpMode == HpMode2024.AVERAGE, { hpMode = HpMode2024.AVERAGE }, label = { Text("Average +${LevelUpMath.averageGain(hitDie) + conMod}") })
                    FilterChip(hpMode == HpMode2024.ROLL, { hpMode = HpMode2024.ROLL }, label = { Text("Roll") })
                }
                if (hpMode == HpMode2024.ROLL) {
                    RollResultView(RollSpec.d(1, hitDie), bonuses = if (conMod != 0) listOf(RollBonus("CON", conMod)) else emptyList(), allowAdvantageToggle = false, onSettled = { rolledDie = it.keptSum })
                }

                if (needsSubclass) {
                    Label2024("Subclass")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        klass!!.subclasses.forEach { s -> FilterChip(subclass == s.id, { subclass = s.id }, label = { Text(s.name) }) }
                    }
                }

                if (needsAsi) {
                    Label2024("Ability Score Improvement")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(asiKind == AsiKind2024.ONE, { asiKind = AsiKind2024.ONE; picks = emptyList() }, label = { Text("+2 to one") })
                        FilterChip(asiKind == AsiKind2024.TWO, { asiKind = AsiKind2024.TWO; picks = emptyList() }, label = { Text("+1 to two") })
                    }
                    val limit = if (asiKind == AsiKind2024.ONE) 1 else 2
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Ability.entries.forEach { a ->
                            val picked = a in picks
                            FilterChip(picked, enabled = payload.abilityScores.score(a) < 20 && (picked || picks.size < limit), onClick = { picks = if (picked) picks - a else picks + a }, label = { Text("${a.abbreviation} ${payload.abilityScores.score(a)}") })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = ready, onClick = {
                onApplyIntent(LevelUp2024(classId, isNew, if (hpMode == HpMode2024.ROLL) HpChoice.Rolled(rolledDie!!) else HpChoice.Average, subclass, asi, klass?.name ?: classId.slugToTitle()))
                onDismiss()
            }) { Text("Level Up") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Label2024(text: String) = Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

private fun Int.signed3(): String = if (this >= 0) "+$this" else "$this"
