package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
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
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.dnd5e.core.HpChoice
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Catalog
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.dnd5e2024.FeatCategory2024
import au.com.evonet.nat20.dnd5e2024.Feats2024
import au.com.evonet.nat20.dnd5e2024.LevelUp2024
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

private enum class HpMode2024 { AVERAGE, ROLL }
private enum class AsiKind2024 { ONE, TWO }
private enum class AdvKind2024 { ASI, FEAT }

/** The 2024 level-up wizard's steps (mirrors the 2014 `LevelUpWizard`). */
private enum class Lu2024Step(val title: String) {
    CLASS("Class"),
    HP("Hit Points"),
    SUBCLASS("Subclass"),
    CHOICES("Choices"),
    REVIEW("Review"),
}

/**
 * The full-screen stepped D&D 5e (2024) level-up wizard (A21 / parity #32):
 * Class → HP → Subclass (conditional) → Choices (conditional) → Review, hosted
 * in the shared [EditorShell] parchment chrome exactly like the creation wizard
 * and the 2014 level-up. Advance a class (or multiclass), choose HP (average or
 * an A16 roll), pick a subclass at the class's subclass level (the 2024 rule for
 * every class), and allocate the ASI-or-feat (with a half-feat's +1) at an
 * advancement level. Fires the existing flat [LevelUp2024] intent unchanged.
 *
 * Android's flow is deliberately **richer than iOS 2024's** minimal subclass +
 * ASI/feat sheet — it keeps the multiclass picker and the average/rolled HP
 * choice, matching Android's 2014 wizard. The 2024 [LevelUp2024] intent models
 * no fighting-style / spell picks at level-up, so (unlike 2014) the Choices step
 * carries only the ASI/feat row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LevelUp2024Wizard(payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit, onDismiss: () -> Unit) {
    val existing = payload.classes.map { it.classId }
    var classId by remember { mutableStateOf(payload.classes.firstOrNull()?.classId ?: "fighter") }
    val isNew = classId !in existing
    val currentClassLevel = payload.classes.firstOrNull { it.classId == classId }?.level ?: 0
    val newClassLevel = currentClassLevel + 1
    val klass = DnD5e2024Catalog.characterClass(classId)
    val hitDie = DnD5eClasses.hitDie(classId)
    val conMod = AbilityScores.modifier(payload.abilityScores.constitution)

    var hpMode by remember { mutableStateOf(HpMode2024.AVERAGE) }
    var rolledDie by remember(classId) { mutableStateOf<Int?>(null) }

    val needsSubclass = klass != null && newClassLevel == klass.subclassLevel &&
        payload.classes.firstOrNull { it.classId == classId }?.subclass == null && klass.subclasses.isNotEmpty()
    var subclass by remember(classId) { mutableStateOf<String?>(null) }

    val needsAsi = LevelUpMath.grantsAbilityScoreImprovement(classId, newClassLevel)
    val nextLevel = payload.level + 1
    val isSpellcaster = payload.classes.any { DnD5e2024Catalog.characterClass(it.classId)?.isCaster == true }
    var advKind by remember { mutableStateOf(AdvKind2024.ASI) }
    var asiKind by remember { mutableStateOf(AsiKind2024.ONE) }
    var picks by remember(classId, advKind) { mutableStateOf<List<Ability>>(emptyList()) }
    var featId by remember(classId, advKind) { mutableStateOf<String?>(null) }
    var halfFeatPick by remember(featId) { mutableStateOf<Ability?>(null) }

    // General feats available at the new level (the ASI itself lives on the ASI branch).
    val availableFeats = remember(nextLevel, isSpellcaster, payload.abilityScores) {
        Feats2024.inCategory(FeatCategory2024.GENERAL)
            .filter { it.id != "ability-score-improvement" && it.isAvailable(nextLevel, payload.abilityScores, isSpellcaster) }
    }
    val pickedFeat = featId?.let { Feats2024.feat(it) }
    val asi: Map<Ability, Int> = when (advKind) {
        AdvKind2024.ASI -> when (asiKind) {
            AsiKind2024.ONE -> picks.take(1).associateWith { 2 }
            AsiKind2024.TWO -> picks.take(2).associateWith { 1 }
        }
        // A half-feat's +1 rides through abilityIncreases, exactly like an ASI.
        AdvKind2024.FEAT -> if (pickedFeat?.grantsAbilityIncrease == true) halfFeatPick?.let { mapOf(it to 1) }.orEmpty() else emptyMap()
    }

    val advReady = when (advKind) {
        AdvKind2024.ASI -> asi.values.sum() == 2
        AdvKind2024.FEAT -> pickedFeat != null && (!pickedFeat.grantsAbilityIncrease || halfFeatPick != null)
    }
    val asiReady = !needsAsi || advReady
    val subclassReady = !needsSubclass || subclass != null
    val hpReady = hpMode == HpMode2024.AVERAGE || rolledDie != null
    val hasChoices = needsAsi
    val canCommit = klass != null && payload.level < DnD5e2024Payload.MAX_LEVEL && asiReady && subclassReady && hpReady

    // Active steps — the two conditionals drop out when not triggered.
    val active = Lu2024Step.entries.filter {
        (it != Lu2024Step.SUBCLASS || needsSubclass) && (it != Lu2024Step.CHOICES || hasChoices)
    }
    var currentStep by remember { mutableStateOf(Lu2024Step.CLASS) }
    var furthest by remember { mutableStateOf(Lu2024Step.CLASS) }
    if (currentStep !in active) currentStep = Lu2024Step.CLASS
    val position = active.indexOf(currentStep).coerceAtLeast(0)

    fun canAdvance(): Boolean = when (currentStep) {
        Lu2024Step.CLASS -> klass != null
        Lu2024Step.HP -> hpReady
        Lu2024Step.SUBCLASS -> subclassReady
        Lu2024Step.CHOICES -> true // lenient — partial commitment is fine
        Lu2024Step.REVIEW -> canCommit
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
        val chosenFeat = if (needsAsi && advKind == AdvKind2024.FEAT) featId else null
        onApplyIntent(
            LevelUp2024(
                classId = classId,
                isNewClass = isNew,
                hpChoice = if (hpMode == HpMode2024.ROLL) HpChoice.Rolled(rolledDie!!) else HpChoice.Average,
                subclass = subclass.takeIf { needsSubclass },
                abilityIncreases = asi,
                feat = chosenFeat,
                className = klass?.name ?: classId.slugToTitle(),
            ),
        )
        onDismiss()
    }

    val palette = MaterialTheme.natPalette
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(palette.parchment)) {
            EditorShell(
                kicker = "Level up · 2024 · Step ${position + 1} of ${active.size}",
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
                    if (currentStep == Lu2024Step.REVIEW) {
                        WizardPrimaryButton("Level up", enabled = canCommit) { commit() }
                    } else {
                        WizardPrimaryButton("Continue", enabled = canAdvance()) { advance() }
                    }
                },
            ) {
                when (currentStep) {
                    // ── Class ──
                    Lu2024Step.CLASS -> {
                        WizardStepSection(
                            "Advance an existing class",
                            subtitle = if (payload.classes.size > 1) "Multiclass — pick which class to advance." else null,
                        )
                        FlowChips2024 {
                            payload.classes.forEach { entry ->
                                val name = DnD5e2024Catalog.characterClass(entry.classId)?.name ?: entry.classId.slugToTitle()
                                WizardChip(
                                    label = "$name ${entry.level} → ${entry.level + 1}",
                                    selected = classId == entry.classId && !isNew,
                                ) { classId = entry.classId }
                            }
                        }
                        val multiclassOptions = DnD5e2024Catalog.classes.filter { it.id !in existing }
                        if (multiclassOptions.isNotEmpty()) {
                            WizardStepSection(
                                "Multiclass into a new class",
                                subtitle = "Picking adds a fresh L1 in that class.",
                            )
                            FlowChips2024 {
                                multiclassOptions.forEach { cls ->
                                    WizardChip(label = "+ ${cls.name}", selected = classId == cls.id && isNew) { classId = cls.id }
                                }
                            }
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
                    Lu2024Step.HP -> {
                        WizardStepSection("Hit point gain", subtitle = "Average is the safe default; rolling adds variance.")
                        WizardSegmented(
                            options = listOf("Average +${LevelUpMath.averageGain(hitDie)}", "Roll d$hitDie"),
                            selectedIndex = if (hpMode == HpMode2024.AVERAGE) 0 else 1,
                        ) { index -> hpMode = if (index == 0) HpMode2024.AVERAGE else HpMode2024.ROLL }
                        if (hpMode == HpMode2024.ROLL) {
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
                                // Bare die only (1..hitDie) — LevelUpMath adds CON itself, so the widget carries
                                // NO CON bonus. (The 2024 path already reads keptSum, so it was never double-
                                // counting, but dropping the CON chip keeps the display honest, matching 2014.)
                                key(classId, hitDie) {
                                    RollResultView(
                                        baseSpec = RollSpec.d(1, hitDie),
                                        allowAdvantageToggle = false,
                                        onSettled = { rolledDie = it.keptSum },
                                    )
                                }
                            }
                        }
                        val dieValue = if (hpMode == HpMode2024.AVERAGE) LevelUpMath.averageGain(hitDie) else (rolledDie ?: 0)
                        val total = maxOf(1, dieValue + conMod)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "This level: +$total HP ($dieValue die + ${conMod.signed2024()} CON)",
                            fontFamily = ImFell,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            color = palette.inkMute,
                        )
                    }

                    // ── Subclass ──
                    Lu2024Step.SUBCLASS -> {
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

                    // ── Choices (ASI / feat — the only per-level choice the 2024 intent models) ──
                    Lu2024Step.CHOICES -> {
                        WizardStepSection("Ability Score Improvement", subtitle = "Take a +2/+1 stat bump, or a General feat instead.")
                        WizardSubSectionCard("Advancement", counter = if (advReady) "chosen" else "not yet chosen") {
                            Spacer(Modifier.height(8.dp))
                            WizardSegmented(listOf("Stat Bump", "Feat"), if (advKind == AdvKind2024.ASI) 0 else 1) {
                                advKind = if (it == 0) AdvKind2024.ASI else AdvKind2024.FEAT
                            }
                            Spacer(Modifier.height(8.dp))
                            if (advKind == AdvKind2024.ASI) {
                                WizardSegmented(listOf("+2 to one", "+1 to two"), if (asiKind == AsiKind2024.ONE) 0 else 1) {
                                    asiKind = if (it == 0) AsiKind2024.ONE else AsiKind2024.TWO
                                    picks = emptyList()
                                }
                                Spacer(Modifier.height(8.dp))
                                val limit = if (asiKind == AsiKind2024.ONE) 1 else 2
                                WizardMultiSelectChips(
                                    Ability.entries.toList(),
                                    isSelected = { it in picks },
                                    label = { "${it.abbreviation} ${payload.abilityScores.score(it)}" },
                                    quotaFull = picks.size >= limit,
                                    enabled = { payload.abilityScores.score(it) < 20 || it in picks },
                                ) { ability ->
                                    picks = if (ability in picks) picks - ability else picks + ability
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
                                            Ability.entries.filter { payload.abilityScores.score(it) < 20 },
                                            { it == halfFeatPick },
                                            { "${it.abbreviation} ${payload.abilityScores.score(it)}" },
                                        ) { halfFeatPick = it }
                                    }
                                }
                            }
                        }
                    }

                    // ── Review ──
                    Lu2024Step.REVIEW -> {
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
                            val dieValue = if (hpMode == HpMode2024.AVERAGE) LevelUpMath.averageGain(hitDie) else (rolledDie ?: 0)
                            val hpTotal = maxOf(1, dieValue + conMod)
                            val method = if (hpMode == HpMode2024.AVERAGE) "Average" else "Rolled $dieValue"
                            ReviewLine("Hit points", "$method — +$hpTotal HP")
                            if (needsSubclass && !subclass.isNullOrEmpty()) {
                                val subName = klass?.subclasses?.firstOrNull { it.id == subclass }?.name ?: subclass!!
                                ReviewLine("Subclass", subName)
                            }
                            if (needsAsi && advKind == AdvKind2024.ASI && asi.isNotEmpty()) {
                                ReviewLine("Ability", asi.entries.joinToString(" · ") { "+${it.value} ${it.key.abbreviation}" })
                            }
                            if (needsAsi && advKind == AdvKind2024.FEAT) {
                                ReviewLine("Feat", pickedFeat?.name ?: "—")
                                if (pickedFeat?.grantsAbilityIncrease == true && halfFeatPick != null) {
                                    ReviewLine("Feat bonus", "+1 ${halfFeatPick!!.abbreviation}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Wrap-flow row for the capsule chips on the Class step. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips2024(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        content = content,
    )
}

private fun Int.signed2024(): String = if (this >= 0) "+$this" else "$this"
