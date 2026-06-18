package au.com.evonet.nat20.ui.codex

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
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.LevelUp
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.dnd5e.core.HpChoice
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.slugToTitle

private enum class HpMode { AVERAGE, ROLL }
private enum class AsiMode { ONE, TWO }

/**
 * The level-up wizard (A11): advance a class (or multiclass), choose HP
 * (average or a roll via the A16 primitive), pick a subclass at the class's
 * subclass level, and allocate an Ability Score Improvement at ASI levels. Spell
 * *selection* stays on the Spells tab; subclass/feat content depth and the
 * deep class catalogues (metamagic / invocations / fighting styles) are later.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LevelUpWizard(payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit, onDismiss: () -> Unit) {
    val existing = payload.classes.map { it.classId }
    var classId by remember { mutableStateOf(payload.classes.firstOrNull()?.classId ?: "fighter") }
    val isNew = classId !in existing
    val currentClassLevel = payload.classes.firstOrNull { it.classId == classId }?.level ?: 0
    val newClassLevel = currentClassLevel + 1
    val klass = DnD5eCatalog.characterClass(classId)
    val hitDie = DnD5eClasses.hitDie(classId)
    val conMod = AbilityScores.modifier(payload.abilityScores.constitution)

    var hpMode by remember { mutableStateOf(HpMode.AVERAGE) }
    var rolledDie by remember(classId) { mutableStateOf<Int?>(null) }

    val needsSubclass = klass != null && newClassLevel == klass.subclassLevel &&
        payload.classes.firstOrNull { it.classId == classId }?.subclass == null && klass.subclasses.isNotEmpty()
    var subclass by remember(classId) { mutableStateOf<String?>(null) }

    val needsAsi = LevelUpMath.grantsAbilityScoreImprovement(classId, newClassLevel)
    var asiMode by remember { mutableStateOf(AsiMode.ONE) }
    var asiPicks by remember(classId) { mutableStateOf<List<Ability>>(emptyList()) }
    val asi: Map<Ability, Int> = when (asiMode) {
        AsiMode.ONE -> asiPicks.take(1).associateWith { 2 }
        AsiMode.TWO -> asiPicks.take(2).associateWith { 1 }
    }

    val asiReady = !needsAsi || asi.values.sum() == 2
    val subclassReady = !needsSubclass || subclass != null
    val hpReady = hpMode == HpMode.AVERAGE || rolledDie != null
    val ready = klass != null && payload.level < DnD5ePayload.MAX_LEVEL && asiReady && subclassReady && hpReady

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Level Up") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Class.
                StepLabel("Class")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DnD5eCatalog.classes.forEach { c ->
                        val label = c.name + (payload.classes.firstOrNull { it.classId == c.id }?.let { " ${it.level}" } ?: " · new")
                        FilterChip(selected = c.id == classId, onClick = { classId = c.id }, label = { Text(label) })
                    }
                }
                Text(
                    "→ ${klass?.name ?: classId.slugToTitle()} $newClassLevel" + if (isNew) " (multiclass)" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                // HP.
                StepLabel("Hit Points (d$hitDie, CON ${conMod.signed()})")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = hpMode == HpMode.AVERAGE, onClick = { hpMode = HpMode.AVERAGE }, label = { Text("Average +${LevelUpMath.averageGain(hitDie) + conMod}") })
                    FilterChip(selected = hpMode == HpMode.ROLL, onClick = { hpMode = HpMode.ROLL }, label = { Text("Roll") })
                }
                if (hpMode == HpMode.ROLL) {
                    RollResultView(
                        baseSpec = RollSpec.d(1, hitDie),
                        bonuses = if (conMod != 0) listOf(au.com.evonet.nat20.dnd5e.core.RollBonus("CON", conMod)) else emptyList(),
                        allowAdvantageToggle = false,
                        onSettled = { rolledDie = it.keptSum },
                    )
                }

                // Subclass.
                if (needsSubclass) {
                    StepLabel("Subclass")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        klass!!.subclasses.forEach { sub ->
                            FilterChip(selected = subclass == sub.id, onClick = { subclass = sub.id }, label = { Text(sub.name) })
                        }
                    }
                }

                // ASI.
                if (needsAsi) {
                    StepLabel("Ability Score Improvement")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = asiMode == AsiMode.ONE, onClick = { asiMode = AsiMode.ONE; asiPicks = emptyList() }, label = { Text("+2 to one") })
                        FilterChip(selected = asiMode == AsiMode.TWO, onClick = { asiMode = AsiMode.TWO; asiPicks = emptyList() }, label = { Text("+1 to two") })
                    }
                    val limit = if (asiMode == AsiMode.ONE) 1 else 2
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Ability.entries.forEach { ability ->
                            val picked = ability in asiPicks
                            val score = payload.abilityScores.score(ability)
                            FilterChip(
                                selected = picked,
                                enabled = score < 20 && (picked || asiPicks.size < limit),
                                onClick = { asiPicks = if (picked) asiPicks - ability else asiPicks + ability },
                                label = { Text("${ability.abbreviation} $score") },
                            )
                        }
                    }
                    Text("Feats as an alternative arrive with the feats catalogue.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = ready,
                onClick = {
                    onApplyIntent(
                        LevelUp(
                            classId = classId,
                            isNewClass = isNew,
                            hpChoice = if (hpMode == HpMode.ROLL) HpChoice.Rolled(rolledDie!!) else HpChoice.Average,
                            subclass = subclass,
                            abilityIncreases = asi,
                            className = klass?.name ?: classId.slugToTitle(),
                        ),
                    )
                    onDismiss()
                },
            ) { Text("Level Up") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun StepLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
}
