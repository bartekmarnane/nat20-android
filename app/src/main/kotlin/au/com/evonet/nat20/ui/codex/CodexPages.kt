package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.ArmorClassCalculator
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.MarkDeathSave
import au.com.evonet.nat20.dnd5e.RaceTraits
import au.com.evonet.nat20.dnd5e.RollDeathSave
import au.com.evonet.nat20.dnd5e.Feats
import au.com.evonet.nat20.dnd5e.effectiveMaxHp
import au.com.evonet.nat20.dnd5e.effectiveSkillProficiencies
import au.com.evonet.nat20.dnd5e.SetInitiative
import au.com.evonet.nat20.dnd5e.SetInspiration
import au.com.evonet.nat20.dnd5e.SpendHitDie
import au.com.evonet.nat20.dnd5e.equippedWeapons
import au.com.evonet.nat20.dnd5e.initiativeBonus
import au.com.evonet.nat20.dnd5e.core.DeathSaveOutcome
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.effectiveAbilityScores
import au.com.evonet.nat20.dnd5e.temporarySaveBonus
import au.com.evonet.nat20.dnd5e.temporarySkillBonus
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.roll.RollDialog
import au.com.evonet.nat20.ui.slugToTitle

// ── Pages ────────────────────────────────────────────────────────────────────

@Composable
internal fun StatsPage(payload: DnD5ePayload, onLevelUp: () -> Unit) {
    val prof = Proficiency.bonus(payload.level)
    val proficientSaves = payload.primaryClass()?.savingThrowAbilities()?.toSet().orEmpty()
    val perceptionProficient = "perception" in payload.effectiveSkillProficiencies
    var check by remember { mutableStateOf<CheckRoll?>(null) }
    val effectiveScores = payload.effectiveAbilityScores
    val isHalfling = payload.race.contains("halfling", ignoreCase = true)
    CodexPage {
        SectionCard("Abilities") {
            Ability.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { ability ->
                        val mod = effectiveScores.modifier(ability)
                        AbilityMedallion(
                            ability, effectiveScores,
                            Modifier
                                .weight(1f)
                                .clickable { check = CheckRoll("${ability.abbreviation} check", listOf(RollBonus(ability.abbreviation, mod)), lucky = isHalfling) },
                        )
                    }
                }
            }
        }
        SectionCard("Proficiency & Senses") {
            StatLine("Proficiency Bonus", prof.signed())
            val passive = 10 + payload.abilityScores.modifier(Ability.WISDOM) + if (perceptionProficient) prof else 0
            StatLine("Passive Perception", passive.toString())
        }
        SectionCard("Saving Throws") {
            Ability.entries.forEach { ability ->
                val proficient = ability in proficientSaves
                val abilityMod = effectiveScores.modifier(ability)
                val effectBonus = payload.temporarySaveBonus(ability)
                val mod = abilityMod + (if (proficient) prof else 0) + effectBonus
                ProficiencyLine(
                    ability.abbreviation, mod.signed(), proficient,
                    onClick = {
                        val bonuses = checkBonuses(ability.abbreviation, abilityMod, proficient, prof) +
                            (if (effectBonus != 0) listOf(RollBonus("Effects", effectBonus)) else emptyList())
                        check = CheckRoll("${ability.abbreviation} save", bonuses, lucky = isHalfling)
                    },
                )
            }
        }
        if (payload.chosenFeats.isNotEmpty()) {
            SectionCard("Feats") {
                payload.chosenFeats.forEach { id ->
                    val feat = Feats.feat(id)
                    Text(feat?.name ?: id.slugToTitle(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    feat?.description?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        if (payload.level < DnD5ePayload.MAX_LEVEL) {
            Button(onClick = onLevelUp, modifier = Modifier.fillMaxWidth()) { Text("Level Up") }
        }
    }
    check?.let { CheckRollDialog(it) { check = null } }
}

@Composable
internal fun SkillsPage(payload: DnD5ePayload) {
    val prof = Proficiency.bonus(payload.level)
    var check by remember { mutableStateOf<CheckRoll?>(null) }
    val effectiveScores = payload.effectiveAbilityScores
    val isHalfling = payload.race.contains("halfling", ignoreCase = true)
    val anySkillBonus = payload.temporarySkillBonus("__any__")
    CodexPage {
        SectionCard("Skills") {
            DnD5eCatalog.skills.forEachIndexed { index, skill ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                val proficient = skill.id in payload.effectiveSkillProficiencies
                val abilityMod = effectiveScores.modifier(skill.ability)
                val effectBonus = payload.temporarySkillBonus(skill.id) + anySkillBonus
                val mod = abilityMod + (if (proficient) prof else 0) + effectBonus
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            val bonuses = checkBonuses(skill.ability.abbreviation, abilityMod, proficient, prof) +
                                (if (effectBonus != 0) listOf(RollBonus("Effects", effectBonus)) else emptyList())
                            check = CheckRoll("${skill.name} check", bonuses, lucky = isHalfling)
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProficiencyDot(proficient)
                    Text(skill.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    Text(
                        skill.ability.abbreviation,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(mod.signed(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    check?.let { CheckRollDialog(it) { check = null } }
}

/** A pending d20 check the roll dialog will present (transient — not journaled). */
internal data class CheckRoll(val title: String, val bonuses: List<RollBonus>, val lucky: Boolean = false)

/** Builds the bonus chips for a d20 check: ability mod + proficiency when proficient. */
private fun checkBonuses(ability: String, abilityMod: Int, proficient: Boolean, prof: Int): List<RollBonus> =
    buildList {
        add(RollBonus(ability, abilityMod))
        if (proficient) add(RollBonus("Proficiency", prof))
    }

@Composable
private fun CheckRollDialog(check: CheckRoll, onDismiss: () -> Unit) {
    RollDialog(
        title = check.title,
        spec = RollSpec.d(1, 20),
        bonuses = check.bonuses,
        allowAdvantageToggle = true,
        luckyReroll = check.lucky,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun CombatPage(character: Character, payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit) {
    val dexMod = payload.abilityScores.modifier(Ability.DEXTERITY)
    val acBreakdown = ArmorClassCalculator.compute(payload)
    var spendHitDie by remember { mutableStateOf(false) }
    var attacking by remember { mutableStateOf(false) }
    var rollingInit by remember { mutableStateOf(false) }
    CodexPage {
        SectionCard("Hit Points") {
            StatLine("Current / Max", "${payload.currentHp} / ${payload.effectiveMaxHp}")
            if (payload.temporaryHp > 0) StatLine("Temporary", "+${payload.temporaryHp}")
        }
        if (payload.currentHp == 0 || !payload.deathSaves.isCleared) {
            DeathSavesCard(payload, onApplyIntent)
        }
        SectionCard("Defense & Movement") {
            StatLine("Armor Class", acBreakdown.total.toString())
            // Spell-out the AC sources so the number is legible (speed defaults to 30).
            Text(
                acBreakdown.rows.joinToString(" + ") { "${it.label} ${it.value.signed()}" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Initiative",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    payload.initiative?.toString() ?: dexMod.signed(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (payload.initiative != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 8.dp),
                )
                TextButton(onClick = { rollingInit = true }) { Text(if (payload.initiative != null) "Reroll" else "Roll") }
                if (payload.initiative != null) {
                    TextButton(onClick = { onApplyIntent(SetInitiative(null)) }) { Text("Clear") }
                }
            }
            StatLine("Speed", "30 ft")
        }
        SectionCard("Attack") {
            Button(onClick = { attacking = true }, enabled = payload.equippedWeapons.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(if (payload.equippedWeapons.isEmpty()) "Equip a weapon to attack" else "Make an attack")
            }
        }
        SectionCard("Hit Dice") {
            StatLine("Available", "${payload.currentHitDice} / ${payload.maxHitDice}")
            payload.classes.forEach { entry ->
                StatLine(entry.classId.slugToTitle(), "${entry.level}d${DnD5eClasses.hitDie(entry.classId)}")
            }
            if (payload.classes.isEmpty()) {
                Text("—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                OutlinedButton(enabled = payload.currentHitDice > 0, onClick = { spendHitDie = true }) {
                    Text("Spend a hit die")
                }
            }
        }
        SectionCard("Inspiration") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (payload.hasInspiration) "You hold Inspiration" else "No Inspiration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (payload.hasInspiration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (payload.hasInspiration) {
                    OutlinedButton(onClick = { onApplyIntent(SetInspiration(false)) }) { Text("Use") }
                } else {
                    OutlinedButton(onClick = { onApplyIntent(SetInspiration(true)) }) { Text("Grant") }
                }
            }
        }
        EffectsSection(payload, onApplyIntent)
        ConditionsSection(payload, onApplyIntent)
        ClassResourcesSection(payload, onApplyIntent)
        CompanionsSection(character, onApplyIntent)
        RestSection(onApplyIntent)
    }
    if (spendHitDie) {
        val hitDie = DnD5eClasses.hitDie(payload.classes.first().classId)
        val conMod = payload.abilityScores.modifier(Ability.CONSTITUTION)
        RollDialog(
            title = "Spend a hit die",
            spec = RollSpec.d(1, hitDie),
            bonuses = if (conMod != 0) listOf(au.com.evonet.nat20.dnd5e.core.RollBonus("CON", conMod)) else emptyList(),
            allowAdvantageToggle = false,
            onSettled = { result -> onApplyIntent(SpendHitDie(maxOf(1, result.total))) },
            onDismiss = { spendHitDie = false },
        )
    }
    if (attacking) {
        AttackSheet(payload, onApplyIntent, onDismiss = { attacking = false })
    }
    if (rollingInit) {
        RollDialog(
            title = "Initiative",
            spec = RollSpec.d(1, 20),
            bonuses = listOf(RollBonus("DEX", payload.initiativeBonus)),
            onSettled = { result -> onApplyIntent(SetInitiative(result.total)) },
            onDismiss = { rollingInit = false },
        )
    }
}

@Composable
private fun DeathSavesCard(payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit) {
    val ds = payload.deathSaves
    var rolling by remember { mutableStateOf(false) }
    SectionCard("Death Saves") {
        val status = when {
            ds.isDead -> "Dead — three failures"
            ds.isStable -> "Stable — three successes"
            else -> "Dying — roll a death save each turn"
        }
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (ds.isDead) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Successes", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "●".repeat(ds.successes) + "○".repeat(3 - ds.successes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Failures", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "✕".repeat(ds.failures) + "·".repeat(3 - ds.failures),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        val resolved = ds.isStable || ds.isDead
        if (!resolved) {
            Button(onClick = { rolling = true }, modifier = Modifier.fillMaxWidth()) { Text("Roll a death save") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !resolved, onClick = { onApplyIntent(MarkDeathSave(DeathSaveOutcome.SUCCESS)) }) { Text("Success") }
            OutlinedButton(enabled = !resolved, onClick = { onApplyIntent(MarkDeathSave(DeathSaveOutcome.FAILURE)) }) { Text("Failure") }
            OutlinedButton(enabled = !ds.isCleared, onClick = { onApplyIntent(MarkDeathSave(DeathSaveOutcome.CLEAR)) }) { Text("Clear") }
        }
        Text(
            "Roll auto-applies the result; the manual buttons are for physical dice.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (rolling) {
        RollDialog(
            title = "Death save",
            spec = RollSpec.d(1, 20),
            allowAdvantageToggle = false,
            onSettled = { result -> result.naturalD20?.let { onApplyIntent(RollDeathSave(it)) } },
            onDismiss = { rolling = false },
        )
    }
}

@Composable
internal fun LorePage(character: Character, payload: DnD5ePayload) {
    CodexPage {
        SectionCard("Identity") {
            StatLine("Name", character.name)
            StatLine("Race", payload.race.takeIf { it.isNotEmpty() }?.slugToTitle() ?: "—")
            StatLine(
                "Class",
                payload.classes.takeIf { it.isNotEmpty() }
                    ?.joinToString(" / ") { "${it.classId.slugToTitle()} ${it.level}" }
                    ?: "—",
            )
            StatLine("Background", payload.background.takeIf { it.isNotEmpty() }?.let { DnD5eCatalog.background(it)?.name ?: it.slugToTitle() } ?: "—")
            StatLine("Level", payload.level.toString())
        }
        val traits = RaceTraits.reminders(payload.race)
        if (traits.isNotEmpty()) {
            SectionCard("Race Traits") {
                traits.forEachIndexed { i, t ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Column(Modifier.padding(vertical = 2.dp)) {
                        Text(t.title, fontWeight = FontWeight.Medium)
                        Text(t.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        SectionCard("Backstory") {
            Text(
                "Alignment and backstory prose arrive with a later content step.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Shared atoms ─────────────────────────────────────────────────────────────

@Composable
internal fun CodexPage(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}

@Composable
internal fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun AbilityMedallion(ability: Ability, scores: AbilityScores, modifier: Modifier = Modifier) {
    val score = scores.score(ability)
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                ability.abbreviation,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                AbilityScores.modifier(score).signed(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(score.toString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

/** "+3" / "-1" / "+0" — 5e always shows the sign on a modifier. */
internal fun Int.signed(): String = if (this >= 0) "+$this" else "$this"

/** The character's primary (first) class, resolved through the catalogue. */
private fun DnD5ePayload.primaryClass() =
    classes.firstOrNull()?.classId?.let(DnD5eCatalog::characterClass)

@Composable
private fun ProficiencyLine(label: String, value: String, proficient: Boolean, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProficiencyDot(proficient)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProficiencyDot(proficient: Boolean) {
    Text(
        if (proficient) "●" else "○",
        style = MaterialTheme.typography.bodySmall,
        color = if (proficient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
    )
}
