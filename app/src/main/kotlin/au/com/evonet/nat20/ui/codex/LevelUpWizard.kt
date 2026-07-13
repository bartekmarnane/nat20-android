package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.Feats
import au.com.evonet.nat20.dnd5e.FightingStyles
import au.com.evonet.nat20.dnd5e.LevelUp
import au.com.evonet.nat20.dnd5e.castableSpellIDs
import au.com.evonet.nat20.dnd5e.isSpellcaster
import au.com.evonet.nat20.dnd5e.maxSpellSlots
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.CastingProgression
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.dnd5e.core.HpChoice
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.editor.EditorShell
import au.com.evonet.nat20.ui.editor.ReviewLine
import au.com.evonet.nat20.ui.editor.ReviewSectionHeader
import au.com.evonet.nat20.ui.editor.WizardChip
import au.com.evonet.nat20.ui.editor.WizardChipsPicker
import au.com.evonet.nat20.ui.editor.WizardMultiSelectChips
import au.com.evonet.nat20.ui.editor.WizardPrimaryButton
import au.com.evonet.nat20.ui.editor.WizardSecondaryButton
import au.com.evonet.nat20.ui.editor.WizardSegmented
import au.com.evonet.nat20.ui.editor.WizardStepSection
import au.com.evonet.nat20.ui.editor.WizardSubSectionCard
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.slugToTitle
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

private enum class HpMode { AVERAGE, ROLL }
private enum class AsiMode { ONE, TWO }
private enum class AdvMode { ASI, FEAT }

/** The stepped level-up wizard's steps (mirrors iOS `LevelUpView.Step`). */
private enum class LuStep(val slug: String, val title: String) {
    CLASS("class", "Class"),
    HP("hp", "Hit Points"),
    SUBCLASS("subclass", "Subclass"),
    CHOICES("choices", "Choices"),
    REVIEW("review", "Review"),
}

/**
 * Standard PHB multiclass ability prerequisites (warn-but-allow). Android has no
 * multiclass-prereq engine catalogue, so this small UI-local table mirrors iOS's
 * advisory banner without any engine work — the intent never blocks on it.
 */
private data class MulticlassPrereq(val summary: String, val met: (AbilityScores) -> Boolean)

private fun a(scores: AbilityScores, ability: Ability) = scores.score(ability) >= 13

private val MULTICLASS_PREREQS: Map<String, MulticlassPrereq> = mapOf(
    "barbarian" to MulticlassPrereq("Strength 13") { a(it, Ability.STRENGTH) },
    "bard" to MulticlassPrereq("Charisma 13") { a(it, Ability.CHARISMA) },
    "cleric" to MulticlassPrereq("Wisdom 13") { a(it, Ability.WISDOM) },
    "druid" to MulticlassPrereq("Wisdom 13") { a(it, Ability.WISDOM) },
    "fighter" to MulticlassPrereq("Strength 13 or Dexterity 13") { a(it, Ability.STRENGTH) || a(it, Ability.DEXTERITY) },
    "monk" to MulticlassPrereq("Dexterity 13 and Wisdom 13") { a(it, Ability.DEXTERITY) && a(it, Ability.WISDOM) },
    "paladin" to MulticlassPrereq("Strength 13 and Charisma 13") { a(it, Ability.STRENGTH) && a(it, Ability.CHARISMA) },
    "ranger" to MulticlassPrereq("Dexterity 13 and Wisdom 13") { a(it, Ability.DEXTERITY) && a(it, Ability.WISDOM) },
    "rogue" to MulticlassPrereq("Dexterity 13") { a(it, Ability.DEXTERITY) },
    "sorcerer" to MulticlassPrereq("Charisma 13") { a(it, Ability.CHARISMA) },
    "warlock" to MulticlassPrereq("Charisma 13") { a(it, Ability.CHARISMA) },
    "wizard" to MulticlassPrereq("Intelligence 13") { a(it, Ability.INTELLIGENCE) },
)

/**
 * The full-screen stepped level-up wizard (A11 / parity #20): Class → HP →
 * Subclass (conditional) → Choices (conditional) → Review, hosted in the shared
 * [EditorShell] chrome exactly like the creation wizard. Advance a class (or
 * multiclass), choose HP (average or an A16 roll), pick a subclass at the
 * class's subclass level, and allocate the choices this level earns (ASI/feat,
 * fighting style, newly-learnable cantrips/spells). Fires the existing flat
 * [LevelUp] intent unchanged. Deeper choice types (Expertise, Metamagic, Pact
 * Boon, Invocations, spell swaps, parameterized feats) are engine-gap follow-ups
 * — see PARITY #20.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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

    val needsStyle = FightingStyles.grantLevel(classId) == newClassLevel && payload.fightingStyles.isEmpty()
    var style by remember(classId) { mutableStateOf<String?>(null) }

    val classCasts = CastingProgression.forClass(classId) != CastingProgression.NONE
    var newCantrips by remember(classId) { mutableStateOf<Set<String>>(emptySet()) }
    var newSpells by remember(classId) { mutableStateOf<Set<String>>(emptySet()) }

    val className = klass?.name
    val maxLvl = payload.maxSpellSlots.keys.maxOrNull() ?: 1
    val cantripPool = remember(classId, payload.cantripsKnown) {
        if (classCasts && className != null) {
            DnD5eCatalog.spellLibrary
                .filter { it.level == 0 && it.classNames.any { c -> c.equals(className, true) } && it.index !in payload.cantripsKnown }
                .sortedBy { it.name }
        } else emptyList()
    }
    val spellPool = remember(classId, payload.castableSpellIDs, maxLvl) {
        if (classCasts && className != null) {
            DnD5eCatalog.spellLibrary
                .filter { it.level in 1..maxLvl && it.classNames.any { c -> c.equals(className, true) } && it.index !in payload.castableSpellIDs }
                .sortedWith(compareBy({ it.level }, { it.name }))
        } else emptyList()
    }
    val hasSpellChoices = cantripPool.isNotEmpty() || spellPool.isNotEmpty()

    val needsAsi = LevelUpMath.grantsAbilityScoreImprovement(classId, newClassLevel)
    var advMode by remember { mutableStateOf(AdvMode.ASI) }
    var asiMode by remember { mutableStateOf(AsiMode.ONE) }
    var asiPicks by remember(classId) { mutableStateOf<List<Ability>>(emptyList()) }
    var featId by remember(classId, advMode) { mutableStateOf<String?>(null) }
    var halfFeatPick by remember(featId) { mutableStateOf<Ability?>(null) }

    val availableFeats = remember(payload.abilityScores, payload.isSpellcaster) {
        Feats.available(payload.abilityScores, payload.isSpellcaster)
    }
    val pickedFeat = featId?.let { Feats.feat(it) }
    val asi: Map<Ability, Int> = when (advMode) {
        AdvMode.ASI -> when (asiMode) {
            AsiMode.ONE -> asiPicks.take(1).associateWith { 2 }
            AsiMode.TWO -> asiPicks.take(2).associateWith { 1 }
        }
        // A half-feat's +1 rides through abilityIncreases, exactly like an ASI.
        AdvMode.FEAT -> if (pickedFeat?.grantsAbilityIncrease == true) halfFeatPick?.let { mapOf(it to 1) }.orEmpty() else emptyMap()
    }

    val hasChoices = needsAsi || needsStyle || hasSpellChoices

    val advReady = when (advMode) {
        AdvMode.ASI -> asi.values.sum() == 2
        AdvMode.FEAT -> pickedFeat != null && (!pickedFeat.grantsAbilityIncrease || halfFeatPick != null)
    }
    val asiReady = !needsAsi || advReady
    val subclassReady = !needsSubclass || subclass != null
    val hpReady = hpMode == HpMode.AVERAGE || rolledDie != null
    val canCommit = klass != null && payload.level < DnD5ePayload.MAX_LEVEL && asiReady && subclassReady && hpReady

    // Active steps — the two conditionals drop out when not triggered.
    val active = LuStep.entries.filter {
        (it != LuStep.SUBCLASS || needsSubclass) && (it != LuStep.CHOICES || hasChoices)
    }
    var currentStep by remember { mutableStateOf(LuStep.CLASS) }
    var furthest by remember { mutableStateOf(LuStep.CLASS) }
    // If the class change dropped the current step out of the active set, fall back to Class.
    if (currentStep !in active) currentStep = LuStep.CLASS
    val position = active.indexOf(currentStep).coerceAtLeast(0)

    fun canAdvance(): Boolean = when (currentStep) {
        LuStep.CLASS -> klass != null
        LuStep.HP -> hpReady
        LuStep.SUBCLASS -> subclassReady
        LuStep.CHOICES -> true // lenient — partial commitment is fine
        LuStep.REVIEW -> canCommit
    }

    fun advance() {
        val next = active.getOrNull(position + 1) ?: return
        currentStep = next
        if (next.ordinal > furthest.ordinal) furthest = next
    }

    fun back() {
        val prev = active.getOrNull(position - 1)
        if (prev == null) onDismiss() else currentStep = prev
    }

    fun commit() {
        onApplyIntent(
            LevelUp(
                classId = classId,
                isNewClass = isNew,
                hpChoice = if (hpMode == HpMode.ROLL) HpChoice.Rolled(rolledDie!!) else HpChoice.Average,
                subclass = subclass.takeIf { needsSubclass },
                abilityIncreases = asi,
                feat = if (needsAsi && advMode == AdvMode.FEAT) featId else null,
                fightingStyle = if (needsStyle) style else null,
                newCantrips = newCantrips.toList(),
                newSpells = newSpells.toList(),
                className = klass?.name ?: classId.slugToTitle(),
            ),
        )
        onDismiss()
    }

    val palette = MaterialTheme.natPalette
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(palette.parchment)) {
            EditorShell(
                kicker = "Level up · Step ${position + 1} of ${active.size}",
                title = currentStep.title,
                stepCount = active.size,
                currentIndex = position,
                onBack = { back() },
                onJump = { target ->
                    active.getOrNull(target)?.let { step ->
                        if (step.ordinal <= furthest.ordinal) currentStep = step
                    }
                },
                footer = {
                    WizardSecondaryButton("Cancel", onDismiss)
                    Spacer(Modifier.weight(1f))
                    if (currentStep == LuStep.REVIEW) {
                        WizardPrimaryButton("Level up", enabled = canCommit) { commit() }
                    } else {
                        WizardPrimaryButton("Continue", enabled = canAdvance()) { advance() }
                    }
                },
            ) {
                when (currentStep) {
                    // ── Class ──
                    LuStep.CLASS -> {
                        WizardStepSection(
                            "Advance an existing class",
                            subtitle = if (payload.classes.size > 1) "Multiclass — pick which class to advance." else null,
                        )
                        FlowChips {
                            payload.classes.forEach { entry ->
                                val name = DnD5eCatalog.characterClass(entry.classId)?.name ?: entry.classId.slugToTitle()
                                WizardChip(
                                    label = "$name ${entry.level} → ${entry.level + 1}",
                                    selected = classId == entry.classId && !isNew,
                                ) { classId = entry.classId }
                            }
                        }
                        val multiclassOptions = DnD5eCatalog.classes.filter { it.id !in existing }
                        if (multiclassOptions.isNotEmpty()) {
                            WizardStepSection(
                                "Multiclass into a new class",
                                subtitle = "Picking adds a fresh L1 in that class. Prereqs are warned-but-not-blocked.",
                            )
                            FlowChips {
                                multiclassOptions.forEach { cls ->
                                    WizardChip(label = "+ ${cls.name}", selected = classId == cls.id && isNew) { classId = cls.id }
                                }
                            }
                        }
                        // Warn-but-allow banner for unmet multiclass prereqs.
                        val prereq = if (isNew) MULTICLASS_PREREQS[classId] else null
                        if (prereq != null && !prereq.met(payload.abilityScores)) {
                            Spacer(Modifier.height(12.dp))
                            PrereqWarningBanner(klass?.name ?: classId.slugToTitle(), prereq.summary)
                        }
                        if (klass != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                klass.name,
                                fontFamily = Cormorant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = palette.ink,
                            )
                            Text(
                                if (isNew) "New class — entering at level 1" else "Level $currentClassLevel → $newClassLevel",
                                fontFamily = ImFell,
                                fontStyle = FontStyle.Italic,
                                fontSize = 13.sp,
                                color = palette.inkMute,
                            )
                        }
                    }

                    // ── Hit Points ──
                    LuStep.HP -> {
                        WizardStepSection("Hit point gain", subtitle = "Average is the safe default; rolling adds variance.")
                        WizardSegmented(
                            options = listOf("Average +${LevelUpMath.averageGain(hitDie)}", "Roll d$hitDie"),
                            selectedIndex = if (hpMode == HpMode.AVERAGE) 0 else 1,
                        ) { index -> hpMode = if (index == 0) HpMode.AVERAGE else HpMode.ROLL }
                        if (hpMode == HpMode.ROLL) {
                            Spacer(Modifier.height(10.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.tile)
                                    .border(1.dp, palette.ink.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                // Bare die only (1..hitDie) — LevelUpMath adds CON itself, so the widget
                                // carries NO CON bonus (dropping it fixes the earlier double-count / validation fail).
                                key(classId, hitDie) {
                                    RollResultView(
                                        baseSpec = RollSpec.d(1, hitDie),
                                        allowAdvantageToggle = false,
                                        onSettled = { rolledDie = it.keptSum },
                                    )
                                }
                            }
                        }
                        val dieValue = if (hpMode == HpMode.AVERAGE) LevelUpMath.averageGain(hitDie) else (rolledDie ?: 0)
                        val total = maxOf(1, dieValue + conMod)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "This level: +$total HP ($dieValue die + ${conMod.signed()} CON)",
                            fontFamily = ImFell,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            color = palette.inkMute,
                        )
                    }

                    // ── Subclass ──
                    LuStep.SUBCLASS -> {
                        WizardStepSection(
                            "Pick your ${klass?.name ?: "class"} subclass",
                            subtitle = "Required at this level — your choice carries through the rest of the campaign.",
                        )
                        val subs = klass?.subclasses.orEmpty()
                        if (subs.isEmpty()) {
                            Text(
                                "No subclass options bundled for this class — type one in to record it.",
                                fontFamily = ImFell,
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp,
                                color = palette.inkMute,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                            WizardTextField("Subclass name…", subclass.orEmpty(), { subclass = it.ifEmpty { null } })
                        } else {
                            WizardChipsPicker(subs, { it.id == subclass }, { it.name }) { subclass = it.id }
                        }
                    }

                    // ── Choices ──
                    LuStep.CHOICES -> {
                        WizardStepSection("Choices at this level", subtitle = "Skip rows you're not ready to commit — they won't apply.")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (needsAsi) {
                                WizardSubSectionCard("Ability Score Improvement", counter = if (advReady) "chosen" else "not yet chosen") {
                                    Spacer(Modifier.height(8.dp))
                                    WizardSegmented(listOf("Stat Bump", "Feat"), if (advMode == AdvMode.ASI) 0 else 1) {
                                        advMode = if (it == 0) AdvMode.ASI else AdvMode.FEAT
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    if (advMode == AdvMode.ASI) {
                                        WizardSegmented(listOf("+2 to one", "+1 to two"), if (asiMode == AsiMode.ONE) 0 else 1) {
                                            asiMode = if (it == 0) AsiMode.ONE else AsiMode.TWO
                                            asiPicks = emptyList()
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        val limit = if (asiMode == AsiMode.ONE) 1 else 2
                                        WizardMultiSelectChips(
                                            Ability.entries.toList(),
                                            isSelected = { it in asiPicks },
                                            label = { "${it.abbreviation} ${payload.abilityScores.score(it)}" },
                                            quotaFull = asiPicks.size >= limit,
                                            enabled = { payload.abilityScores.score(it) < 20 || it in asiPicks },
                                        ) { ability ->
                                            asiPicks = if (ability in asiPicks) asiPicks - ability else asiPicks + ability
                                        }
                                    } else {
                                        WizardChipsPicker(availableFeats, { it.id == featId }, { it.name }) { featId = it.id; halfFeatPick = null }
                                        pickedFeat?.let { f ->
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                f.description,
                                                fontFamily = Cormorant,
                                                fontStyle = FontStyle.Italic,
                                                fontSize = 13.sp,
                                                color = palette.inkSoft,
                                            )
                                            if (f.grantsAbilityIncrease) {
                                                Spacer(Modifier.height(6.dp))
                                                Text("+1 ability score", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.inkMute)
                                                Spacer(Modifier.height(4.dp))
                                                WizardChipsPicker(
                                                    f.halfFeatAbilities.filter { payload.abilityScores.score(it) < 20 },
                                                    { it == halfFeatPick },
                                                    { "${it.abbreviation} ${payload.abilityScores.score(it)}" },
                                                ) { halfFeatPick = it }
                                            }
                                        }
                                    }
                                }
                            }
                            if (needsStyle) {
                                WizardSubSectionCard("Fighting Style", counter = if (style != null) "1/1 picked" else "0/1 picked") {
                                    Spacer(Modifier.height(8.dp))
                                    WizardChipsPicker(FightingStyles.all, { it.id == style }, { it.name }) { picked ->
                                        style = if (style == picked.id) null else picked.id
                                    }
                                    style?.let { id ->
                                        FightingStyles.style(id)?.let { fs ->
                                            Spacer(Modifier.height(6.dp))
                                            Text(fs.description, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkSoft)
                                        }
                                    }
                                }
                            }
                            if (cantripPool.isNotEmpty()) {
                                WizardSubSectionCard("New Cantrip", counter = "${newCantrips.size} picked") {
                                    Spacer(Modifier.height(8.dp))
                                    WizardMultiSelectChips(
                                        cantripPool,
                                        isSelected = { it.index in newCantrips },
                                        label = { it.name },
                                        quotaFull = false,
                                    ) { s -> newCantrips = if (s.index in newCantrips) newCantrips - s.index else newCantrips + s.index }
                                }
                            }
                            if (spellPool.isNotEmpty()) {
                                WizardSubSectionCard("New Spells", counter = "${newSpells.size} picked") {
                                    Spacer(Modifier.height(8.dp))
                                    WizardMultiSelectChips(
                                        spellPool,
                                        isSelected = { it.index in newSpells },
                                        label = { "${it.name} · L${it.level}" },
                                        quotaFull = false,
                                    ) { s -> newSpells = if (s.index in newSpells) newSpells - s.index else newSpells + s.index }
                                }
                            }
                        }
                    }

                    // ── Review ──
                    LuStep.REVIEW -> {
                        ReviewSectionHeader("Confirm")
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.tile)
                                .border(1.dp, palette.ink.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(10.dp),
                        ) {
                            ReviewLine("Class", "${klass?.name ?: classId.slugToTitle()} $currentClassLevel → $newClassLevel" + if (isNew) " (multiclass)" else "")
                            val dieValue = if (hpMode == HpMode.AVERAGE) LevelUpMath.averageGain(hitDie) else (rolledDie ?: 0)
                            val hpTotal = maxOf(1, dieValue + conMod)
                            val method = if (hpMode == HpMode.AVERAGE) "Average" else "Rolled $dieValue"
                            ReviewLine("Hit points", "$method — +$hpTotal HP")
                            if (needsSubclass && !subclass.isNullOrEmpty()) {
                                val subName = klass?.subclasses?.firstOrNull { it.id == subclass }?.name ?: subclass!!
                                ReviewLine("Subclass", subName)
                            }
                            if (needsAsi && advMode == AdvMode.ASI && asi.isNotEmpty()) {
                                ReviewLine("Ability", asi.entries.joinToString(" · ") { "+${it.value} ${it.key.abbreviation}" })
                            }
                            if (needsAsi && advMode == AdvMode.FEAT) {
                                ReviewLine("Feat", pickedFeat?.name ?: "—")
                            }
                            if (needsStyle && style != null) {
                                ReviewLine("Fighting style", FightingStyles.style(style!!)?.name ?: style!!)
                            }
                            if (newCantrips.isNotEmpty()) {
                                ReviewLine("New cantrips", cantripPool.filter { it.index in newCantrips }.joinToString(" · ") { it.name })
                            }
                            if (newSpells.isNotEmpty()) {
                                ReviewLine("New spells", spellPool.filter { it.index in newSpells }.joinToString(" · ") { it.name })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Wrap-flow row for the capsule chips on the Class step. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        content = content,
    )
}

/** Warn-but-allow banner for an unmet multiclass ability prerequisite. */
@Composable
private fun PrereqWarningBanner(className: String, requirement: String) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.danger.copy(alpha = 0.08f))
            .border(1.dp, palette.danger.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(10.dp),
    ) {
        Text(
            "MULTICLASS PREREQ NOT MET",
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = palette.danger,
        )
        Text(
            "$className requires $requirement. You can still level up, but the choice breaks RAW.",
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp,
            color = palette.ink.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
