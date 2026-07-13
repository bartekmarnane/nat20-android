package au.com.evonet.nat20.ui.actions

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.RaceTraits
import au.com.evonet.nat20.dnd5e.RollCheck
import au.com.evonet.nat20.dnd5e.Skill
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.DeathSaveOutcome
import au.com.evonet.nat20.dnd5e.core.DeathSaves
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e.effectiveAbilityScores
import au.com.evonet.nat20.dnd5e.hasExpertise
import au.com.evonet.nat20.dnd5e.skillProficiencyMultiplier
import au.com.evonet.nat20.dnd5e.temporarySaveBonus
import au.com.evonet.nat20.dnd5e.temporarySkillBonus
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The slice-B ROLL pickers (parity #19): structured skill-check, raw ability-
 * check, saving-throw (with "Save against" context chips + race-trait
 * advantage), and death-save (roll + mark) pickers. Ports of the iOS
 * `SkillCheckPicker` / `AbilityCheckPicker` / `SavingThrowPicker` /
 * `DeathSavePicker`. All embed the A16 `RollResultView` inline: pick →
 * roll → judge → note → commit a [RollCheck] (journal-only).
 */

/** The player's pass/fail call on a check that has no in-app DC. */
internal enum class CheckOutcome { FAIL, UNJUDGED, PASS }

/** The pieces of a settled d20 the pickers carry to the intent. */
private data class SettledRoll(val d20: Int?, val total: Int)

// ── Bonus math (mirrors the Stats/Skills pages) ────────────────────────────────

private fun skillCheckBonuses(payload: DnD5ePayload, skill: Skill): List<RollBonus> {
    val prof = Proficiency.bonus(payload.level)
    val profMult = payload.skillProficiencyMultiplier(skill.id)
    val abilityMod = payload.effectiveAbilityScores.modifier(skill.ability)
    val expertise = payload.hasExpertise(skill.id) && profMult > 0
    val effectBonus = payload.temporarySkillBonus(skill.id) + payload.temporarySkillBonus("__any__")
    return buildList {
        add(RollBonus(skill.ability.abbreviation, abilityMod))
        if (profMult > 0) add(RollBonus(if (expertise) "Expertise" else "Proficiency", prof * profMult))
        if (effectBonus != 0) add(RollBonus("Effects", effectBonus))
    }
}

private fun abilityCheckBonuses(payload: DnD5ePayload, ability: Ability): List<RollBonus> =
    listOf(RollBonus(ability.abbreviation, payload.effectiveAbilityScores.modifier(ability)))

private fun isSaveProficient(payload: DnD5ePayload, ability: Ability): Boolean =
    ability in DnD5eCatalog.characterClass(payload.classes.firstOrNull()?.classId ?: "")
        ?.savingThrowAbilities().orEmpty()

private fun savingThrowBonuses(payload: DnD5ePayload, ability: Ability): List<RollBonus> {
    val abilityMod = payload.effectiveAbilityScores.modifier(ability)
    val effectBonus = payload.temporarySaveBonus(ability)
    return buildList {
        add(RollBonus(ability.abbreviation, abilityMod))
        if (isSaveProficient(payload, ability)) add(RollBonus("Proficiency", Proficiency.bonus(payload.level)))
        if (effectBonus != 0) add(RollBonus("Effects", effectBonus))
    }
}

// ── Skill check ────────────────────────────────────────────────────────────────

/**
 * Structured skill check: skill chip → inline d20 with the skill's bonus chips
 * → outcome chips → optional note → [RollCheck] (iOS `SkillCheckPicker`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SkillCheckPicker(
    payload: DnD5ePayload,
    onCancel: () -> Unit,
    onCommit: (RollCheck) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var skill by remember { mutableStateOf<Skill?>(null) }
    var rolled by remember { mutableStateOf<SettledRoll?>(null) }
    var outcome by remember { mutableStateOf(CheckOutcome.UNJUDGED) }
    var note by remember { mutableStateOf("") }
    val sortedSkills = remember { DnD5eCatalog.skills.sortedBy { it.name } }

    ActionPickerShell(kicker = "Roll", title = "Skill check", onCancel = onCancel) {
        PickerSection("Skill", top = 6.dp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            sortedSkills.forEach { s ->
                val active = skill == s
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(if (active) palette.accent else palette.tile)
                        .border(1.dp, if (active) palette.accent else palette.ink.copy(alpha = 0.2f), CircleShape)
                        .clickable {
                            skill = s
                            rolled = null
                            outcome = CheckOutcome.UNJUDGED
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        s.name,
                        fontFamily = Cormorant,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (active) palette.cream else palette.ink,
                    )
                    Text(
                        s.ability.abbreviation,
                        fontFamily = Cinzel,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = if (active) palette.cream.copy(alpha = 0.7f) else palette.inkMute,
                    )
                }
            }
        }

        PickerSection("Roll")
        RollPanel(
            selectionKey = skill?.id,
            spec = RollSpec.d(1, 20),
            bonuses = skill?.let { skillCheckBonuses(payload, it) } ?: emptyList(),
            lucky = RaceTraits.hasHalflingLuck(payload.race),
            prompt = "Pick a skill above to roll.",
            onSettled = { rolled = it; outcome = CheckOutcome.UNJUDGED },
            onReset = { rolled = null; outcome = CheckOutcome.UNJUDGED },
        )

        if (rolled != null) {
            PickerSection("How did it land?")
            CheckOutcomeChips(outcome) { outcome = it }
            PickerSection("Note (optional)")
            WizardTextField("To force the door, to read the captain, …", note, { if (it.length <= 240) note = it })
        }

        CheckCommitButton(
            visible = skill != null && rolled != null,
            outcome = outcome,
            passLabel = "Record success",
            failLabel = "Record failure",
        ) {
            onCommit(
                RollCheck(
                    label = "${skill!!.name} check",
                    total = rolled!!.total,
                    naturalD20 = rolled!!.d20,
                    judgedSuccess = outcome.judged,
                    note = note.trim().takeIf { it.isNotEmpty() },
                ),
            )
        }
    }
}

// ── Ability check ──────────────────────────────────────────────────────────────

/**
 * Raw ability check — no governing skill or proficiency (iOS
 * `AbilityCheckPicker`): ability chip → d20 with the mod chip → judge → note.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AbilityCheckPicker(
    payload: DnD5ePayload,
    onCancel: () -> Unit,
    onCommit: (RollCheck) -> Unit,
) {
    var ability by remember { mutableStateOf<Ability?>(null) }
    var rolled by remember { mutableStateOf<SettledRoll?>(null) }
    var outcome by remember { mutableStateOf(CheckOutcome.UNJUDGED) }
    var note by remember { mutableStateOf("") }

    ActionPickerShell(kicker = "Roll", title = "Ability check", onCancel = onCancel) {
        PickerSection("Ability", top = 6.dp)
        AbilityChipRow(
            selected = ability,
            proficientSaves = emptySet(),
            onSelect = {
                ability = it
                rolled = null
                outcome = CheckOutcome.UNJUDGED
            },
        )

        PickerSection("Roll")
        RollPanel(
            selectionKey = ability?.name,
            spec = RollSpec.d(1, 20),
            bonuses = ability?.let { abilityCheckBonuses(payload, it) } ?: emptyList(),
            lucky = RaceTraits.hasHalflingLuck(payload.race),
            prompt = "Pick an ability above to roll.",
            onSettled = { rolled = it; outcome = CheckOutcome.UNJUDGED },
            onReset = { rolled = null; outcome = CheckOutcome.UNJUDGED },
        )

        if (rolled != null) {
            PickerSection("How did it land?")
            CheckOutcomeChips(outcome) { outcome = it }
            PickerSection("Note (optional)")
            WizardTextField("To hold breath, to shove, to recall, …", note, { if (it.length <= 240) note = it })
        }

        CheckCommitButton(
            visible = ability != null && rolled != null,
            outcome = outcome,
            passLabel = "Record success",
            failLabel = "Record failure",
        ) {
            onCommit(
                RollCheck(
                    label = "${ability!!.abbreviation} check",
                    total = rolled!!.total,
                    naturalD20 = rolled!!.d20,
                    judgedSuccess = outcome.judged,
                    note = note.trim().takeIf { it.isNotEmpty() },
                ),
            )
        }
    }
}

// ── Saving throw ───────────────────────────────────────────────────────────────

private val SAVE_DAMAGE_TYPES = listOf(
    "Acid", "Bludgeoning", "Cold", "Fire", "Force", "Lightning", "Necrotic",
    "Piercing", "Poison", "Psychic", "Radiant", "Slashing", "Thunder",
)

private val SAVE_CONDITIONS = listOf(
    "Blinded", "Charmed", "Deafened", "Frightened", "Grappled", "Incapacitated",
    "Paralyzed", "Petrified", "Poisoned", "Prone", "Restrained", "Stunned", "Unconscious",
)

/**
 * Saving throw with structured "Save against" context (iOS
 * `SavingThrowPicker`): the damage-type / condition / magical-source chips
 * light up race-trait auto-advantage (Dwarven Resilience, Fey Ancestry, Gnome
 * Cunning, Brave) and flip the d20 to advantage.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SavingThrowPicker(
    payload: DnD5ePayload,
    onCancel: () -> Unit,
    onCommit: (RollCheck) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var ability by remember { mutableStateOf<Ability?>(null) }
    var damageType by remember { mutableStateOf<String?>(null) }
    var condition by remember { mutableStateOf<String?>(null) }
    var magicalSource by remember { mutableStateOf(false) }
    var rolled by remember { mutableStateOf<SettledRoll?>(null) }
    var outcome by remember { mutableStateOf(CheckOutcome.UNJUDGED) }
    var note by remember { mutableStateOf("") }

    val proficientSaves = Ability.entries.filter { isSaveProficient(payload, it) }.toSet()
    val advantageHint = ability?.let {
        RaceTraits.advantageOnSave(payload.race, it, damageType, condition, magicalSource)
    }

    fun resetRoll() {
        rolled = null
        outcome = CheckOutcome.UNJUDGED
    }

    ActionPickerShell(kicker = "Roll", title = "Saving throw", onCancel = onCancel) {
        PickerSection("Ability", top = 6.dp)
        AbilityChipRow(
            selected = ability,
            proficientSaves = proficientSaves,
            onSelect = { ability = it; resetRoll() },
        )

        if (ability != null) {
            PickerSection("Save against (optional)")
            ContextLabel("Damage type")
            ContextChipStrip(SAVE_DAMAGE_TYPES, damageType) { damageType = it; resetRoll() }
            PickerGap(8.dp)
            ContextLabel("Condition")
            ContextChipStrip(SAVE_CONDITIONS, condition) { condition = it; resetRoll() }
            PickerGap(8.dp)
            SmallCapsChip("Magical source", active = magicalSource) {
                magicalSource = !magicalSource
                resetRoll()
            }
        }

        PickerSection("Roll")
        RollPanel(
            selectionKey = ability?.let { "${it.name}-$damageType-$condition-$magicalSource" },
            spec = if (advantageHint != null) RollSpec.advantage(20) else RollSpec.d(1, 20),
            bonuses = ability?.let { savingThrowBonuses(payload, it) } ?: emptyList(),
            lucky = RaceTraits.hasHalflingLuck(payload.race),
            prompt = "Pick an ability above to roll.",
            onSettled = { rolled = it; outcome = CheckOutcome.UNJUDGED },
            onReset = ::resetRoll,
        )
        advantageHint?.let { hint ->
            Text(
                "${hint.traitName} · ${hint.reason}",
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = palette.inkMute,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (rolled != null) {
            PickerSection("Did it land?")
            CheckOutcomeChips(outcome, passLabel = "Saved", failLabel = "Failed") { outcome = it }
            PickerSection("Note (optional)")
            WizardTextField("vs. dragon's breath, vs. hold person, …", note, { if (it.length <= 240) note = it })
        }

        CheckCommitButton(
            visible = ability != null && rolled != null,
            outcome = outcome,
            passLabel = "Record save",
            failLabel = "Record failed save",
        ) {
            val context = listOfNotNull(
                damageType?.let { "vs ${it.lowercase()}" },
                condition?.let { "vs $it" },
                "magical source".takeIf { magicalSource },
                advantageHint?.let { "${it.traitName} advantage" },
            ).joinToString(", ").takeIf { it.isNotEmpty() }
            val composedNote = listOfNotNull(note.trim().takeIf { it.isNotEmpty() }, context)
                .joinToString(" · ").takeIf { it.isNotEmpty() }
            onCommit(
                RollCheck(
                    label = "${ability!!.abbreviation} save",
                    total = rolled!!.total,
                    naturalD20 = rolled!!.d20,
                    judgedSuccess = outcome.judged,
                    note = composedNote,
                ),
            )
        }
    }
}

// ── Death save ─────────────────────────────────────────────────────────────────

/**
 * Death saves (iOS `DeathSavePicker` + Android's atomic `RollDeathSave`):
 * tally banner, an inline d20 whose settled face arms a "Record rolled save"
 * commit (RAW applied atomically — nat 1 = two failures, nat 20 = revive at
 * 1 HP), plus manual Success / Failure / Clear tiles for physical dice.
 */
@Composable
internal fun DeathSavePicker(
    deathSaves: DeathSaves,
    currentHp: Int,
    hasHalflingLuck: Boolean,
    onCancel: () -> Unit,
    onRoll: (d20: Int) -> Unit,
    onMark: (DeathSaveOutcome) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var rolledD20 by remember { mutableStateOf<Int?>(null) }
    val resolved = deathSaves.isStable || deathSaves.isDead

    ActionPickerShell(kicker = "Vitals", title = "Mark death save", onCancel = onCancel) {
        PickerGap(10.dp)
        // Current tally.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(palette.tile)
                .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TallyColumn("Successes", deathSaves.successes, palette.accentGold, Modifier.weight(1f))
            TallyColumn("Failures", deathSaves.failures, palette.danger, Modifier.weight(1f))
        }
        val status = when {
            deathSaves.isDead -> "Dead — three failures."
            deathSaves.isStable -> "Stable — three successes."
            currentHp == 0 -> "Dying — roll a death save each turn."
            else -> "Not dying."
        }
        Text(
            status,
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 11.sp,
            color = if (deathSaves.isDead) palette.danger else palette.inkMute,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )

        if (!resolved) {
            PickerSection("Roll")
            RollPanel(
                selectionKey = "death-save",
                spec = RollSpec.d(1, 20),
                bonuses = emptyList(),
                lucky = hasHalflingLuck,
                allowAdvantageToggle = false,
                prompt = "",
                onSettled = { rolledD20 = it.d20 },
                onReset = { rolledD20 = null },
            )
            rolledD20?.let { d20 ->
                val hint = when {
                    d20 == 20 -> "Natural 20 — revive at 1 HP and clear the tally."
                    d20 == 1 -> "Natural 1 — counts as two failures."
                    d20 >= 10 -> "10 or higher — one success."
                    else -> "Under 10 — one failure."
                }
                Text(
                    hint,
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = 11.sp,
                    color = palette.inkMute,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                PickerGap(10.dp)
                PrimaryActionButton(
                    label = "Record rolled save (d20 = $d20)",
                    isDanger = d20 < 10 && d20 != 20,
                    onClick = { onRoll(d20) },
                )
            }
        }

        PickerSection("Mark")
        PickerHelpText("The manual marks are for physical dice.", Modifier.padding(bottom = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DeathSaveTile("Success", palette.accentGold, enabled = deathSaves.successes < 3 && !resolved) {
                onMark(DeathSaveOutcome.SUCCESS)
            }
            DeathSaveTile("Failure", palette.danger, enabled = deathSaves.failures < 3 && !deathSaves.isDead) {
                onMark(DeathSaveOutcome.FAILURE)
            }
            DeathSaveTile("Clear", palette.inkMute, enabled = !deathSaves.isCleared) {
                onMark(DeathSaveOutcome.CLEAR)
            }
        }
    }
}

@Composable
private fun TallyColumn(label: String, count: Int, tone: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.5.sp,
            color = MaterialTheme.natPalette.inkMute,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (i < count) tone else Color.Transparent)
                        .border(1.2.dp, tone.copy(alpha = if (i < count) 1f else 0.4f), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun DeathSaveTile(label: String, tone: Color, enabled: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(4.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.tileStrong.copy(alpha = if (enabled) 1f else 0.4f))
            .border(1.2.dp, tone.copy(alpha = if (enabled) 0.4f else 0.15f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(tone.copy(alpha = if (enabled) 1f else 0.35f)))
        Spacer(Modifier.width(12.dp))
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.5.sp,
            color = palette.ink.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.weight(1f),
        )
        Text("›", fontSize = 14.sp, color = palette.inkMute)
    }
}

// ── Shared bits ────────────────────────────────────────────────────────────────

private val CheckOutcome.judged: Boolean?
    get() = when (this) {
        CheckOutcome.PASS -> true
        CheckOutcome.FAIL -> false
        CheckOutcome.UNJUDGED -> null
    }

@Composable
private fun CheckOutcomeChips(
    outcome: CheckOutcome,
    passLabel: String = "Success",
    failLabel: String = "Failure",
    onSelect: (CheckOutcome) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    OutcomeChipRow(
        selection = outcome,
        options = listOf(
            OutcomeOption(CheckOutcome.FAIL, failLabel, palette.danger),
            OutcomeOption(CheckOutcome.UNJUDGED, "Unjudged", palette.accent),
            OutcomeOption(CheckOutcome.PASS, passLabel, palette.accentGold),
        ),
        onSelect = onSelect,
    )
}

@Composable
private fun CheckCommitButton(
    visible: Boolean,
    outcome: CheckOutcome,
    passLabel: String,
    failLabel: String,
    onClick: () -> Unit,
) {
    if (!visible) return
    PickerGap(18.dp)
    PrimaryActionButton(
        label = when (outcome) {
            CheckOutcome.PASS -> passLabel
            CheckOutcome.FAIL -> failLabel
            CheckOutcome.UNJUDGED -> "Record roll"
        },
        isDanger = outcome == CheckOutcome.FAIL,
        onClick = onClick,
    )
}

/** CIN-abbreviation ability chips; a small gold seal marks save proficiency. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilityChipRow(
    selected: Ability?,
    proficientSaves: Set<Ability>,
    onSelect: (Ability) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Ability.entries.forEach { ability ->
            val active = selected == ability
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(if (active) palette.accent else palette.tile)
                    .border(1.dp, if (active) palette.accent else palette.ink.copy(alpha = 0.2f), CircleShape)
                    .clickable { onSelect(ability) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    ability.abbreviation,
                    fontFamily = Cinzel,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp,
                    color = if (active) palette.cream else palette.ink,
                )
                if (ability in proficientSaves) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (active) palette.cream.copy(alpha = 0.85f) else palette.accentGold),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextLabel(label: String) {
    Text(
        label.uppercase(),
        fontFamily = Cinzel,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        color = MaterialTheme.natPalette.inkMute,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

/** Toggleable single-select context chip strip; re-tapping the active chip clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextChipStrip(options: List<String>, selection: String?, onSelect: (String?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            PickerChip(option, active = selection == option) {
                onSelect(if (selection == option) null else option)
            }
        }
    }
}

/**
 * The shared inline dice mount (the spec's dice pattern): nothing until a
 * selection exists, then a `RollResultView` keyed by the selection so a swap
 * remounts the dice and tears down stale judgment.
 */
@Composable
private fun RollPanel(
    selectionKey: String?,
    spec: RollSpec,
    bonuses: List<RollBonus>,
    lucky: Boolean,
    prompt: String,
    allowAdvantageToggle: Boolean = true,
    onSettled: (SettledRoll) -> Unit,
    onReset: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    if (selectionKey == null) {
        Text(
            prompt,
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp,
            color = palette.inkMute,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        )
        return
    }
    key(selectionKey, spec) {
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            RollResultView(
                baseSpec = spec,
                bonuses = bonuses,
                allowAdvantageToggle = allowAdvantageToggle,
                luckyReroll = lucky,
                onSettled = { onSettled(SettledRoll(it.naturalD20, it.total)) },
                onReset = onReset,
            )
        }
    }
}
