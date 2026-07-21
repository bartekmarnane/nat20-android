package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.ArmorClassCalculator
import au.com.evonet.nat20.dnd5e.AttackMath
import au.com.evonet.nat20.dnd5e.CancelEffect
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.Feats
import au.com.evonet.nat20.dnd5e.FightingStyles
import au.com.evonet.nat20.dnd5e.Invocations
import au.com.evonet.nat20.dnd5e.MarkDeathSave
import au.com.evonet.nat20.dnd5e.Metamagics
import au.com.evonet.nat20.dnd5e.PactBoons
import au.com.evonet.nat20.dnd5e.RaceTraits
import au.com.evonet.nat20.dnd5e.RollCheck
import au.com.evonet.nat20.dnd5e.RollDeathSave
import au.com.evonet.nat20.dnd5e.SetInitiative
import au.com.evonet.nat20.dnd5e.SpendHitDie
import au.com.evonet.nat20.dnd5e.WeaponProperties
import au.com.evonet.nat20.dnd5e.advantageDescriptors
import au.com.evonet.nat20.dnd5e.availableResourcePools
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.DeathSaves
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollResult
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e.effectiveAbilityScores
import au.com.evonet.nat20.dnd5e.effectiveDamageResistances
import au.com.evonet.nat20.dnd5e.effectiveMaxHp
import au.com.evonet.nat20.dnd5e.effectiveSkillProficiencies
import au.com.evonet.nat20.dnd5e.effectiveSpeed
import au.com.evonet.nat20.dnd5e.equippedWeapons
import au.com.evonet.nat20.dnd5e.expertiseEligibleSkills
import au.com.evonet.nat20.dnd5e.expertiseSlots
import au.com.evonet.nat20.dnd5e.hasExpertise
import au.com.evonet.nat20.dnd5e.initiativeBonus
import au.com.evonet.nat20.dnd5e.invocationsKnownCount
import au.com.evonet.nat20.dnd5e.maxPactSlots
import au.com.evonet.nat20.dnd5e.metamagicKnownCount
import au.com.evonet.nat20.dnd5e.pactBoonAvailable
import au.com.evonet.nat20.dnd5e.pactSlotLevel
import au.com.evonet.nat20.dnd5e.skillProficiencyMultiplier
import au.com.evonet.nat20.dnd5e.temporarySaveBonus
import au.com.evonet.nat20.dnd5e.temporarySkillBonus
import au.com.evonet.nat20.dnd5e.totalCurrentSlots
import au.com.evonet.nat20.dnd5e.totalMaxSlots
import au.com.evonet.nat20.dnd5e.warlockLevel
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.actions.AttackPicker
import au.com.evonet.nat20.ui.actions.DeathSavePicker
import au.com.evonet.nat20.ui.roll.RollDialog
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.slugToTitle
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.DiamondShape
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

// ── Stats ────────────────────────────────────────────────────────────────────

/**
 * iOS Stats order: chip row · HP compact · Abilities medallions · Saving Throws
 * strip · Effects · Resources · Defenses · Languages · Inspiration banner.
 * Android keeps tap-to-roll on medallions/saves (iOS is display-only; pending
 * #19) and its Class Choices + Level Up extras at the end.
 */
@Composable
internal fun StatsPage(character: Character, payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit, onSave: (Character) -> Unit, onLevelUp: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val prof = Proficiency.bonus(payload.level)
    val proficientSaves = payload.primaryClass()?.savingThrowAbilities()?.toSet().orEmpty()
    var check by remember { mutableStateOf<CheckRoll?>(null) }
    var acBreakdown by remember { mutableStateOf(false) }
    var effectDetail by remember { mutableStateOf<au.com.evonet.nat20.dnd5e.core.ActiveEffect?>(null) }
    val effectiveScores = payload.effectiveAbilityScores
    val isHalfling = payload.race.contains("halfling", ignoreCase = true)

    CodexPage {
        // Chip row: Init / AC / Prof / Speed.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Init", payload.initiative?.toString() ?: payload.initiativeBonus.signed(), Modifier.weight(1f))
            StatChip("AC", payload.armorClass.toString(), Modifier.weight(1f), onClick = { acBreakdown = true })
            StatChip("Prof", prof.signed(), Modifier.weight(1f))
            StatChip("Speed", "${payload.effectiveSpeed} ft", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        HPBlock(payload.currentHp, payload.effectiveMaxHp, payload.temporaryHp, compact = true)

        SectionHead("Abilities", top = 28.dp, bottom = 16.dp)
        Ability.entries.chunked(3).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                row.forEach { ability ->
                    val mod = effectiveScores.modifier(ability)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AbilityMedallion(
                            abbrev = ability.abbreviation,
                            score = effectiveScores.score(ability),
                            modifierValue = mod,
                            onClick = { check = CheckRoll("${ability.abbreviation} check", listOf(RollBonus(ability.abbreviation, mod)), lucky = isHalfling) },
                        )
                    }
                }
            }
        }

        SectionHead("Saving Throws", top = 24.dp, bottom = 16.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Ability.entries.forEach { ability ->
                val proficient = ability in proficientSaves
                val abilityMod = effectiveScores.modifier(ability)
                val effectBonus = payload.temporarySaveBonus(ability)
                val mod = abilityMod + (if (proficient) prof else 0) + effectBonus
                SaveCell(
                    abbrev = ability.abbreviation,
                    bonus = mod,
                    proficient = proficient,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val bonuses = checkBonuses(ability.abbreviation, abilityMod, proficient, prof) +
                            (if (effectBonus != 0) listOf(RollBonus("Effects", effectBonus)) else emptyList())
                        check = CheckRoll("${ability.abbreviation} save", bonuses, lucky = isHalfling)
                    },
                )
            }
        }

        if (payload.activeEffects.isNotEmpty()) {
            SectionHead("Effects", top = 24.dp, bottom = 12.dp)
            EffectStrip(payload.activeEffects) { effectDetail = it }
        }

        if (payload.hasVisibleConditions()) {
            SectionHead("Conditions", top = 24.dp, bottom = 12.dp)
            ConditionsDisplayStrip(payload)
        }

        val pools = payload.availableResourcePools()
        if (pools.isNotEmpty()) {
            SectionHead("Resources", top = 24.dp, bottom = 12.dp)
            ResourcePoolStrip(pools.map { Triple(it.abbreviation, it.current, it.max) })
        }

        val resistances = payload.effectiveDamageResistances.sorted()
        val advantages = payload.advantageDescriptors
        if (resistances.isNotEmpty() || advantages.isNotEmpty()) {
            SectionHead("Defenses", top = 24.dp, bottom = 12.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (resistances.isNotEmpty()) DefenseCategory("Resistance", resistances, palette.accent)
                if (advantages.isNotEmpty()) DefenseCategory("Advantage", advantages, palette.accentGold)
            }
        }

        SectionHead("Languages", top = 24.dp, bottom = 12.dp)
        val languages = DnD5eCatalog.race(payload.race)?.languages.orEmpty()
        Text(
            languages.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "—",
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = palette.ink,
        )

        if (payload.hasInspiration) {
            InspirationBanner(Modifier.padding(top = 14.dp))
        }

        // ── Android extras (no iOS Stats equivalent; pending #19 actions layer) ──
        ClassChoicesSection(character, payload, onSave)
        if (payload.level < DnD5ePayload.MAX_LEVEL) {
            Spacer(Modifier.height(20.dp))
            CodexButton("Level Up", Modifier.fillMaxWidth(), onClick = onLevelUp)
        }
        Spacer(Modifier.height(8.dp))
    }

    check?.let { CheckRollDialog(it, onApplyIntent) { check = null } }
    if (acBreakdown) AcBreakdownDialog(payload) { acBreakdown = false }
    effectDetail?.let { e ->
        EffectDetailDialog(
            effect = e,
            onCancelEffect = { onApplyIntent(CancelEffect(e.id)); effectDetail = null },
            onDismiss = { effectDetail = null },
        )
    }
}

/** Tappable strip of active-effect chips (detail dialog on tap, as on Combat). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EffectStrip(effects: List<au.com.evonet.nat20.dnd5e.core.ActiveEffect>, onSelect: (au.com.evonet.nat20.dnd5e.core.ActiveEffect) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        effects.forEach { e -> CodexEffectChip(e) { onSelect(e) } }
    }
}

/** Display tiles for point-pool resources (spend lives on Combat, pending #19). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourcePoolStrip(pools: List<Triple<String, Int, Int>>) {
    val palette = MaterialTheme.natPalette
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        pools.forEach { (label, current, max) ->
            val shape = RoundedCornerShape(3.dp)
            Column(
                Modifier
                    .clip(shape)
                    .background(palette.tile)
                    .border(1.dp, palette.accent.copy(alpha = 0.2f), shape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(label.uppercase(), fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.inkMute)
                Text("$current / $max", fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = palette.accent)
            }
        }
    }
}

/** A defense category row: small-caps label + tinted entry chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefenseCategory(label: String, entries: List<String>, tone: androidx.compose.ui.graphics.Color) {
    val palette = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.inkMute,
            modifier = Modifier.width(96.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.forEach { entry ->
                val shape = RoundedCornerShape(3.dp)
                Text(
                    entry.replaceFirstChar { it.uppercase() },
                    fontFamily = Cormorant,
                    fontSize = 13.sp,
                    color = tone,
                    modifier = Modifier
                        .clip(shape)
                        .border(1.dp, tone.copy(alpha = 0.4f), shape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun AcBreakdownDialog(payload: DnD5ePayload, onDismiss: () -> Unit) {
    val breakdown = ArmorClassCalculator.compute(payload)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Armor Class ${breakdown.total}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                breakdown.rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(row.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(row.value.signed(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/**
 * The deep-class build choices (A11): Sorcerer Metamagic, Warlock Invocations +
 * Pact Boon, Rogue/Bard Expertise. Each is a character-building edit (like
 * spells-known), so it persists via a direct [onSave] payload copy rather than a
 * journaled intent. Android extra — iOS makes these picks in Level Up (#20).
 */
@Composable
private fun ClassChoicesSection(character: Character, payload: DnD5ePayload, onSave: (Character) -> Unit) {
    val metamagicSlots = payload.metamagicKnownCount
    val invocationSlots = payload.invocationsKnownCount
    val expertiseSlots = payload.expertiseSlots
    val pactBoon = payload.pactBoonAvailable
    if (metamagicSlots == 0 && invocationSlots == 0 && expertiseSlots == 0 && !pactBoon) return

    fun save(updated: DnD5ePayload) = onSave(character.copy(payload = updated))

    SectionHead("Class Choices", top = 24.dp, bottom = 12.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (metamagicSlots > 0) {
            ChoiceGroup("Metamagic (${payload.metamagicKnown.size}/$metamagicSlots)", Metamagics.all, { it.id }, { it.name },
                selected = payload.metamagicKnown.toSet(),
                canAddMore = payload.metamagicKnown.size < metamagicSlots,
            ) { id, on -> save(payload.copy(metamagicKnown = toggle(payload.metamagicKnown, id, on))) }
        }
        if (pactBoon) {
            ChoiceGroup("Pact Boon", PactBoons.all, { it.id }, { it.name },
                selected = setOfNotNull(payload.pactBoon),
                canAddMore = true,
                single = true,
            ) { id, on -> save(payload.copy(pactBoon = if (on) id else null)) }
        }
        if (invocationSlots > 0) {
            val available = Invocations.all.filter {
                it.isAvailable(
                    payload.warlockLevel,
                    payload.pactBoon,
                    (payload.cantripsKnown + payload.spellsKnown.values.flatten() + payload.preparedSpells.values.flatten()).toSet(),
                ) || it.id in payload.invocationsKnown
            }
            ChoiceGroup("Eldritch Invocations (${payload.invocationsKnown.size}/$invocationSlots)", available, { it.id }, { it.name },
                selected = payload.invocationsKnown.toSet(),
                canAddMore = payload.invocationsKnown.size < invocationSlots,
            ) { id, on -> save(payload.copy(invocationsKnown = toggle(payload.invocationsKnown, id, on))) }
        }
        if (expertiseSlots > 0) {
            val eligible = payload.expertiseEligibleSkills.map { it to (DnD5eCatalog.skill(it)?.name ?: it) }
            ChoiceGroup("Expertise (${payload.expertiseSkills.size}/$expertiseSlots)", eligible, { it.first }, { it.second },
                selected = payload.expertiseSkills.toSet(),
                canAddMore = payload.expertiseSkills.size < expertiseSlots,
            ) { id, on -> save(payload.copy(expertiseSkills = toggle(payload.expertiseSkills, id, on).filter { payload.hasExpertise(it) || it in payload.expertiseEligibleSkills })) }
        }
    }
}

private fun toggle(list: List<String>, id: String, on: Boolean): List<String> =
    if (on) (list + id).distinct() else list - id

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceGroup(
    title: String,
    options: List<T>,
    id: (T) -> String,
    label: (T) -> String,
    selected: Set<String>,
    canAddMore: Boolean,
    single: Boolean = false,
    onToggle: (String, Boolean) -> Unit,
) {
    Text(
        title.uppercase(),
        fontFamily = Cinzel,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        color = MaterialTheme.natPalette.accent,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { opt ->
            val key = id(opt)
            val on = key in selected
            androidx.compose.material3.FilterChip(
                selected = on,
                enabled = on || canAddMore,
                onClick = { onToggle(key, if (single) true else !on) },
                label = { Text(label(opt)) },
            )
        }
    }
}

// ── Skills ───────────────────────────────────────────────────────────────────

/**
 * iOS Skills: passive-perception callout, then 18 dash-separated rows with
 * proficiency pips. Android keeps tap-to-roll on each row (pending #19).
 */
@Composable
internal fun SkillsPage(payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit) {
    val palette = MaterialTheme.natPalette
    val prof = Proficiency.bonus(payload.level)
    var check by remember { mutableStateOf<CheckRoll?>(null) }
    val effectiveScores = payload.effectiveAbilityScores
    val isHalfling = payload.race.contains("halfling", ignoreCase = true)
    val anySkillBonus = payload.temporarySkillBonus("__any__")

    CodexPage {
        // Passive perception callout.
        val perceptionMult = payload.skillProficiencyMultiplier("perception")
        val passive = 10 + effectiveScores.modifier(Ability.WISDOM) + prof * perceptionMult +
            payload.temporarySkillBonus("perception") + anySkillBonus
        val shape = RoundedCornerShape(3.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(palette.tileStrong)
                .border(1.dp, palette.accent.copy(alpha = 0.33f), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "PASSIVE PERCEPTION",
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 2.5.sp,
                color = palette.inkMute,
            )
            Spacer(Modifier.weight(1f))
            Text(
                passive.toString(),
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                color = palette.accent,
            )
        }

        SectionHead("All Skills", top = 24.dp, bottom = 12.dp)
        DnD5eCatalog.skills.forEachIndexed { index, skill ->
            if (index > 0) DashedRule()
            val proficient = skill.id in payload.effectiveSkillProficiencies
            val expertise = payload.hasExpertise(skill.id) && proficient
            val profMult = payload.skillProficiencyMultiplier(skill.id)
            val abilityMod = effectiveScores.modifier(skill.ability)
            val effectBonus = payload.temporarySkillBonus(skill.id) + anySkillBonus
            val mod = abilityMod + prof * profMult + effectBonus
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        val bonuses = buildList {
                            add(RollBonus(skill.ability.abbreviation, abilityMod))
                            if (profMult > 0) add(RollBonus(if (expertise) "Expertise" else "Proficiency", prof * profMult))
                            if (effectBonus != 0) add(RollBonus("Effects", effectBonus))
                        }
                        check = CheckRoll("${skill.name} check", bonuses, lucky = isHalfling)
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkillPip(proficient, expertise)
                Text(
                    skill.name,
                    fontFamily = Cormorant,
                    fontWeight = if (proficient) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 15.sp,
                    color = if (proficient) palette.ink else palette.inkSoft,
                    modifier = Modifier.padding(start = 10.dp),
                )
                if (effectBonus != 0) {
                    Text(
                        " ${effectBonus.signed()} effect",
                        fontFamily = ImFell,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp,
                        color = palette.accentGold,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    skill.ability.abbreviation.uppercase(),
                    fontFamily = Cinzel,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    color = palette.inkMute,
                    modifier = Modifier.width(32.dp),
                )
                Text(
                    mod.signed(),
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = if (proficient) palette.accent else palette.ink,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(36.dp),
                )
            }
        }
        Text(
            "○ untrained · ● proficient · ◆ expertise — tap a skill to roll",
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 11.sp,
            color = palette.inkMute,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    check?.let { CheckRollDialog(it, onApplyIntent) { check = null } }
}

/** A pending d20 check the roll dialog will present. */
internal data class CheckRoll(val title: String, val bonuses: List<RollBonus>, val lucky: Boolean = false)

/** Builds the bonus chips for a d20 check: ability mod + proficiency when proficient. */
private fun checkBonuses(ability: String, abilityMod: Int, proficient: Boolean, prof: Int): List<RollBonus> =
    buildList {
        add(RollBonus(ability, abilityMod))
        if (proficient) add(RollBonus("Proficiency", prof))
    }

/**
 * Presents a d20 check (A16 roll primitive) and, optionally, judges it against a
 * DC and logs the pass/fail to the journal (A15) via [RollCheck]. A "Set DC"
 * toggle reveals a ± stepper; "Log to journal" appears once the dice settle.
 */
@Composable
private fun CheckRollDialog(check: CheckRoll, onApplyIntent: (CharacterIntent) -> Unit, onDismiss: () -> Unit) {
    var settled by remember { mutableStateOf<RollResult?>(null) }
    var useDc by remember { mutableStateOf(false) }
    var dc by remember { mutableStateOf(15) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(check.title) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RollResultView(
                    baseSpec = RollSpec.d(1, 20),
                    bonuses = check.bonuses,
                    allowAdvantageToggle = true,
                    luckyReroll = check.lucky,
                    onSettled = { settled = it },
                    // Re-rolling (or correcting a hand-entered face) invalidates
                    // the result, so the log button waits for the new one.
                    onReset = { settled = null },
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Judge vs DC", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { useDc = !useDc }) { Text(if (useDc) "On" else "Off") }
                }
                if (useDc) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { dc = (dc - 1).coerceAtLeast(1) }) { Text("−") }
                        Text("DC $dc", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { dc = (dc + 1).coerceAtMost(40) }) { Text("+") }
                    }
                    settled?.let { r ->
                        val pass = r.total >= dc
                        Text(
                            if (pass) "Success — ${r.total} ≥ $dc" else "Failure — ${r.total} < $dc",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (pass) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            val result = settled
            if (result != null) {
                TextButton(onClick = {
                    onApplyIntent(RollCheck(check.title, result.total, result.naturalD20, if (useDc) dc else null))
                    onDismiss()
                }) { Text("Log to journal") }
            } else {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = if (settled != null) {
            { TextButton(onClick = onDismiss) { Text("Close") } }
        } else {
            null
        },
    )
}

// ── Combat ───────────────────────────────────────────────────────────────────

/**
 * iOS Combat order: HP large · Armor/Init/Speed chips · Hit Dice + Death Saves
 * tiles · Combat Features · Attacks table. Android's interactive extras (attack
 * flow, slot expend, inspiration setter, effects, conditions, class resources,
 * rest) follow after Attacks, restyled — all pending #19's actions layer.
 */
@Composable
internal fun CombatPage(character: Character, payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit) {
    val palette = MaterialTheme.natPalette
    var spendHitDie by remember { mutableStateOf(false) }
    var attacking by remember { mutableStateOf(false) }
    var rollingInit by remember { mutableStateOf(false) }
    var initDialog by remember { mutableStateOf(false) }
    var acBreakdown by remember { mutableStateOf(false) }
    var deathDialog by remember { mutableStateOf(false) }

    CodexPage {
        HPBlock(payload.currentHp, payload.effectiveMaxHp, payload.temporaryHp, compact = false)

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Armor", payload.armorClass.toString(), Modifier.weight(1f), onClick = { acBreakdown = true })
            StatChip(
                "Init",
                payload.initiative?.toString() ?: payload.initiativeBonus.signed(),
                Modifier.weight(1f),
                highlight = payload.initiative != null,
                onClick = { if (payload.initiative == null) rollingInit = true else initDialog = true },
            )
            StatChip("Speed", "${payload.effectiveSpeed} ft", Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HitDiceTile(payload, Modifier.width(130.dp), onClick = { if (payload.currentHitDice > 0 && payload.classes.isNotEmpty()) spendHitDie = true })
            DeathSavesTile(payload.deathSaves, Modifier.weight(1f), onClick = { deathDialog = true })
        }

        val styles = payload.fightingStyles.mapNotNull(FightingStyles::style)
        if (styles.isNotEmpty()) {
            SectionHead("Combat Features", top = 22.dp, bottom = 10.dp)
            styles.forEach { FeatureBlock(it.name, it.description) }
        }

        SectionHead("Attacks", top = 22.dp, bottom = 8.dp)
        AttacksTable(payload)
        Spacer(Modifier.height(10.dp))
        CodexButton(
            if (payload.equippedWeapons.isEmpty()) "Equip a weapon to attack" else "Make an attack",
            Modifier.fillMaxWidth(),
            enabled = payload.equippedWeapons.isNotEmpty(),
            onClick = { attacking = true },
        )

        // The former inline management sections (rest, inspiration setter,
        // slot-expend, conditions, class resources, effects) now live in the Act
        // sheet (parity #19 slice C). Combat is display-only past the attacks
        // table; the "Make an attack" button stays as an on-page convenience
        // (an Android extra — iOS has no on-page attack button).
        Spacer(Modifier.height(8.dp))
    }

    if (spendHitDie) {
        val hitDie = DnD5eClasses.hitDie(payload.classes.first().classId)
        val conMod = payload.abilityScores.modifier(Ability.CONSTITUTION)
        RollDialog(
            title = "Spend a hit die",
            spec = RollSpec.d(1, hitDie),
            bonuses = if (conMod != 0) listOf(RollBonus("CON", conMod)) else emptyList(),
            allowAdvantageToggle = false,
            onCommit = { result -> onApplyIntent(SpendHitDie(maxOf(1, result.total))) },
            onDismiss = { spendHitDie = false },
        )
    }
    if (attacking) {
        // The shared #19 attack picker (slice B) — the old AttackSheet dialog is gone.
        AttackPicker(
            payload = payload,
            onCancel = { attacking = false },
            onCommit = { intent ->
                onApplyIntent(intent)
                attacking = false
            },
        )
    }
    if (rollingInit) {
        RollDialog(
            title = "Initiative",
            spec = RollSpec.d(1, 20),
            bonuses = listOf(RollBonus("DEX", payload.initiativeBonus)),
            onCommit = { result -> onApplyIntent(SetInitiative(result.total)) },
            onDismiss = { rollingInit = false },
        )
    }
    if (initDialog) {
        AlertDialog(
            onDismissRequest = { initDialog = false },
            title = { Text("Initiative") },
            text = { Text("Rolled initiative: ${payload.initiative}", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { initDialog = false; rollingInit = true }) { Text("Reroll") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onApplyIntent(SetInitiative(null)); initDialog = false }) { Text("Clear") }
                    TextButton(onClick = { initDialog = false }) { Text("Close") }
                }
            },
        )
    }
    if (acBreakdown) AcBreakdownDialog(payload) { acBreakdown = false }
    if (deathDialog) {
        // The shared #19 death-save picker (slice B) — the old manage dialog is gone.
        DeathSavePicker(
            deathSaves = payload.deathSaves,
            currentHp = payload.currentHp,
            hasHalflingLuck = RaceTraits.hasHalflingLuck(payload.race),
            onCancel = { deathDialog = false },
            onRoll = { d20 ->
                onApplyIntent(RollDeathSave(d20))
                deathDialog = false
            },
            onMark = { outcome ->
                onApplyIntent(MarkDeathSave(outcome))
                deathDialog = false
            },
        )
    }
}

/** Hit-dice tile: remaining over total with the class die (tap to spend). */
@Composable
private fun HitDiceTile(payload: DnD5ePayload, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    val dieLabel = payload.classes.singleOrNull()
        ?.let { "/${payload.maxHitDice}d${DnD5eClasses.hitDie(it.classId)}" }
        ?: "/${payload.maxHitDice}"
    Column(
        modifier
            .clip(shape)
            .background(palette.tile)
            .border(1.dp, palette.accent.copy(alpha = 0.2f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("HIT DICE", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.inkMute)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                payload.currentHitDice.toString(),
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = palette.accent,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                dieLabel,
                fontFamily = Cormorant,
                fontSize = 12.sp,
                color = palette.inkSoft.copy(alpha = 0.6f),
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

/** Death-saves tile: three success circles + three failure diamonds (tap to manage). */
@Composable
private fun DeathSavesTile(ds: DeathSaves, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    Column(
        modifier
            .clip(shape)
            .background(palette.tile)
            .border(1.dp, palette.accent.copy(alpha = 0.2f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("DEATH SAVES", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.inkMute)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .then(
                            if (i < ds.successes) Modifier.background(palette.accent)
                            else Modifier.border(1.dp, palette.accent.copy(alpha = 0.53f), androidx.compose.foundation.shape.CircleShape),
                        ),
                )
            }
            Spacer(Modifier.width(8.dp))
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(DiamondShape)
                        .then(
                            if (i < ds.failures) Modifier.background(palette.danger)
                            else Modifier.border(1.dp, palette.danger.copy(alpha = 0.5f), DiamondShape),
                        ),
                )
            }
        }
    }
}

/** iOS attacks table: Weapon / Atk / Damage / Range columns over hairline rules. */
@Composable
private fun AttacksTable(payload: DnD5ePayload) {
    val palette = MaterialTheme.natPalette
    val attacks = payload.equippedWeapons.mapNotNull { item ->
        AttackMath.forWeapon(item, payload)?.let { item.weapon!! to it }
    }
    if (attacks.isEmpty()) {
        DashedNotice("No weapons equipped — ready one on the Items tab.")
        return
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            AttackHeaderCell("WEAPON", Modifier.weight(1f))
            AttackHeaderCell("ATK", Modifier.width(44.dp))
            AttackHeaderCell("DAMAGE", Modifier.width(80.dp))
            AttackHeaderCell("RANGE", Modifier.width(70.dp))
        }
        attacks.forEach { (weapon, attack) ->
            HairlineRule()
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        attack.name,
                        fontFamily = Cormorant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = palette.ink,
                        maxLines = 1,
                    )
                    Text(
                        if (weapon.kind == WeaponProperties.Kind.RANGED) "ranged · ${attack.damageType}" else "melee · ${attack.damageType}",
                        fontFamily = ImFell,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp,
                        color = palette.inkMute,
                        maxLines = 1,
                    )
                }
                Text(
                    attack.attackBonuses.sumOf { it.value }.signed(),
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = palette.accent,
                    modifier = Modifier.width(44.dp),
                )
                val damageBonus = attack.damageBonuses.sumOf { it.value }
                Text(
                    weapon.damageDice + (if (damageBonus != 0) damageBonus.signed() else ""),
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = palette.ink,
                    modifier = Modifier.width(80.dp),
                )
                Text(
                    weapon.normalRange?.let { n -> "$n${weapon.longRange?.let { "/$it" } ?: ""} ft" } ?: "Melee",
                    fontFamily = EbGaramond,
                    fontSize = 12.sp,
                    color = palette.inkSoft,
                    modifier = Modifier.width(70.dp),
                )
            }
        }
        HairlineRule()
    }
}

@Composable
private fun AttackHeaderCell(text: String, modifier: Modifier) {
    Text(
        text,
        fontFamily = Cinzel,
        fontSize = 11.sp,
        letterSpacing = 1.8.sp,
        color = MaterialTheme.natPalette.inkMute,
        modifier = modifier,
    )
}

// ── Lore ─────────────────────────────────────────────────────────────────────

/**
 * iOS Lore order: identity grid · Bearing · Manner quote blocks · Backstory ·
 * Allies · Minions · Features & Traits. Android's payload has no Player/XP,
 * bearing, or allies fields yet — those sections are omitted (payload gap).
 * Minions hosts the companions/summons cards moved from Combat.
 */
@Composable
internal fun LorePage(character: Character, payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit) {
    val palette = MaterialTheme.natPalette
    CodexPage {
        // Identity grid (2-col). Player / XP are an iOS payload feature — omitted.
        Row(Modifier.fillMaxWidth()) {
            IdentityCell(
                "Background",
                payload.background.takeIf { it.isNotEmpty() }?.let { DnD5eCatalog.background(it)?.name ?: it.slugToTitle() },
                Modifier.weight(1f),
            )
            IdentityCell("Alignment", payload.alignment, Modifier.weight(1f))
        }

        val manner = listOf(
            "Personality" to payload.personality,
            "Ideals" to payload.ideals,
            "Bonds" to payload.bonds,
            "Flaws" to payload.flaws,
        ).filter { it.second.isNotBlank() }
        if (manner.isNotEmpty()) {
            SectionHead("Manner", top = 22.dp, bottom = 10.dp)
            manner.forEach { (label, text) -> QuoteBlock(label, text) }
        }

        if (payload.backstory.isNotBlank()) {
            SectionHead("Backstory", top = 22.dp, bottom = 10.dp)
            Text(
                payload.backstory,
                fontFamily = EbGaramond,
                fontSize = 13.sp,
                color = palette.inkSoft,
            )
        }
        if (manner.isEmpty() && payload.backstory.isBlank() && payload.alignment == null) {
            Spacer(Modifier.height(14.dp))
            DashedNotice("No alignment or backstory yet — add them from Edit Character.")
        }

        // Minions — companions & summons (moved from Combat per iOS).
        MinionsSection(character, onApplyIntent)

        // Features & traits: racial traits + feats + background feature.
        val features = buildList<Pair<String, String>> {
            val race = DnD5eCatalog.race(payload.race)
            race?.traits?.forEach { add(it.name to it.body) }
            RaceTraits.reminders(payload.race).forEach { r ->
                if (none { it.first.equals(r.title, ignoreCase = true) }) add(r.title to r.detail)
            }
            payload.chosenFeats.forEach { id ->
                val feat = Feats.feat(id)
                add((feat?.name ?: id.slugToTitle()) to (feat?.description ?: ""))
            }
            payload.background.takeIf { it.isNotEmpty() }?.let { bg ->
                DnD5eCatalog.background(bg)?.feature?.let { add(it.name to it.body) }
            }
        }
        if (features.isNotEmpty()) {
            SectionHead("Features & Traits", top = 22.dp, bottom = 10.dp)
            features.forEach { (title, body) -> FeatureBlock(title, body) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ── Shared page scaffold & helpers ───────────────────────────────────────────

/** Scrollable page body: h22 gutters, no implicit spacing (SectionHeads own rhythm). */
@Composable
internal fun CodexPage(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, bottom = 16.dp),
    ) {
        content()
    }
}

/** "+3" / "-1" / "+0" — 5e always shows the sign on a modifier. */
internal fun Int.signed(): String = if (this >= 0) "+$this" else "$this"

/** The character's primary (first) class, resolved through the catalogue. */
private fun DnD5ePayload.primaryClass() =
    classes.firstOrNull()?.classId?.let(DnD5eCatalog::characterClass)
