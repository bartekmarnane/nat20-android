package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.Background
import au.com.evonet.nat20.dnd5e.CharacterClass
import au.com.evonet.nat20.dnd5e.ClassEntry
import au.com.evonet.nat20.dnd5e.CustomRaceLibrary
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.DnD5eRuleset
import au.com.evonet.nat20.dnd5e.Feats
import au.com.evonet.nat20.dnd5e.FightingStyles
import au.com.evonet.nat20.dnd5e.InventoryItem
import au.com.evonet.nat20.dnd5e.ItemKind
import au.com.evonet.nat20.dnd5e.Race
import au.com.evonet.nat20.dnd5e.Spell
import au.com.evonet.nat20.dnd5e.StartingEquipment
import au.com.evonet.nat20.dnd5e.effectiveMaxHp
import au.com.evonet.nat20.dnd5e.withFullSpellSlots
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.CastingProgression
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.Diamond
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette
import java.time.Instant
import kotlin.math.max

/**
 * The multi-step D&D 5e creation/edit wizard (A8, restyled to the iOS parchment
 * atoms in the parity pass). Steps follow the iOS order: Name → Race → Class →
 * Background → Abilities → Advancements (above level 1) → Skills →
 * [Fighting Style — Android-only step, see PARITY.md] → Equipment → Spells
 * (casters) → Manner → Review, reading the SRD catalogues (`DnD5eCatalog`) and
 * materialising a draft into a `Character`. Hosted in the shared [EditorShell]
 * chrome; the unified creation flow prefixes the Ruleset step (`stepOffset = 1`,
 * `onExitFirstStep` returns to it), while the edit route hosts it standalone.
 */
private enum class WizStep(val title: String) {
    NAME("Name"), RACE("Race"), CLASS("Class"), BACKGROUND("Background"),
    ABILITIES("Abilities"), ADVANCEMENTS("Advancements"), SKILLS("Skills"),
    FIGHTING_STYLE("Fighting Style"), EQUIPMENT("Equipment"), SPELLS("Spells"),
    MANNER("Manner"), REVIEW("Review"),
}

/** The shape of a single advancement-level choice for an above-level-1 build (A11). */
private enum class AdvKind(val label: String) { ASI_ONE("+2 one"), ASI_TWO("+1 two"), FEAT("Feat") }

private data class AdvState(
    val kind: AdvKind = AdvKind.ASI_ONE,
    val abilities: List<Ability> = emptyList(),
    val featId: String? = null,
    val half: Ability? = null,
) {
    /** The ability bumps this choice contributes (+2/one, +1/two, or a half-feat's +1). */
    fun bumps(): Map<Ability, Int> = when (kind) {
        AdvKind.ASI_ONE -> abilities.take(1).associateWith { 2 }
        AdvKind.ASI_TWO -> abilities.take(2).associateWith { 1 }
        AdvKind.FEAT -> half?.let { mapOf(it to 1) }.orEmpty()
    }

    val chosenFeat: String? get() = featId.takeIf { kind == AdvKind.FEAT }

    fun isComplete(): Boolean = when (kind) {
        AdvKind.ASI_ONE -> abilities.size == 1
        AdvKind.ASI_TWO -> abilities.size == 2
        AdvKind.FEAT -> featId != null && (Feats.feat(featId)?.grantsAbilityIncrease != true || half != null)
    }
}

/** Levels at or below [level] where [classId] grants an Ability Score Improvement. */
private fun asiLevels(classId: String?, level: Int): List<Int> =
    if (classId == null) emptyList() else (1..level).filter { LevelUpMath.grantsAbilityScoreImprovement(classId, it) }

@Composable
fun DnD5eWizardScreen(
    existing: Character?,
    onSave: (Character) -> Unit,
    onCancel: () -> Unit,
    stepOffset: Int = 0,
    onExitFirstStep: (() -> Unit)? = null,
) {
    val source = existing?.payload as? DnD5ePayload

    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var raceId by rememberSaveable { mutableStateOf(source?.race?.ifEmpty { null }) }
    var classId by rememberSaveable { mutableStateOf(source?.classes?.firstOrNull()?.classId) }
    var level by rememberSaveable { mutableIntStateOf(source?.classes?.firstOrNull()?.level ?: 1) }
    var backgroundId by rememberSaveable { mutableStateOf(source?.background?.ifEmpty { null }) }
    var base by remember { mutableStateOf(initialBaseScores(source)) }
    var chosenSkills by remember { mutableStateOf(classChosenSkills(source)) }
    var fightingStyle by rememberSaveable { mutableStateOf(source?.fightingStyles?.firstOrNull()) }
    var chosenCantrips by remember { mutableStateOf(source?.cantripsKnown?.toSet().orEmpty()) }
    var chosenSpells by remember { mutableStateOf((source?.spellsKnown.orEmpty().values.flatten() + source?.preparedSpells.orEmpty().values.flatten()).toSet()) }
    var subclass by rememberSaveable { mutableStateOf(source?.classes?.firstOrNull()?.subclass) }
    var advs by remember { mutableStateOf<Map<Int, AdvState>>(emptyMap()) }
    // Manner + Equipment state (iOS parity steps).
    var alignment by rememberSaveable { mutableStateOf(source?.alignment) }
    var personality by rememberSaveable { mutableStateOf(source?.personality.orEmpty()) }
    var ideals by rememberSaveable { mutableStateOf(source?.ideals.orEmpty()) }
    var bonds by rememberSaveable { mutableStateOf(source?.bonds.orEmpty()) }
    var flaws by rememberSaveable { mutableStateOf(source?.flaws.orEmpty()) }
    var backstory by rememberSaveable { mutableStateOf(source?.backstory.orEmpty()) }
    var inventory by remember { mutableStateOf(source?.inventory.orEmpty()) }
    // Edit mode must not re-seed a kit the character already carries.
    var equipmentSeeded by rememberSaveable { mutableStateOf(source?.inventory?.isNotEmpty() == true) }
    // Pencil-jump from Review: the footer collapses to a single "Done" back to Review.
    var editingFromReview by rememberSaveable { mutableStateOf(false) }
    // Homebrew races (parity #11): collecting the library re-renders the Race
    // step (picker + detail card) whenever a homebrew is saved or deleted.
    val customRaces by CustomRaceLibrary.races.collectAsState()
    val allRaces = remember(customRaces) { DnD5eCatalog.races }
    var showRaceForm by rememberSaveable { mutableStateOf(false) }
    var editingCustomRace by remember { mutableStateOf<Race?>(null) }

    val race = raceId?.let(DnD5eCatalog::race)
    val klass = classId?.let(DnD5eCatalog::characterClass)
    val background = backgroundId?.let(DnD5eCatalog::background)
    val raceBonus = race?.abilityBonuses().orEmpty()
    val finalScores = base.applying(raceBonus)
    val backgroundSkills = background?.grantedSkills.orEmpty()

    // The Fighting Style step appears only when the chosen class grants one by the chosen level.
    val grantsStyle = klass != null && FightingStyles.grantsBy(klass.id, level)
    // The Spells step appears only for classes that cast at level 1 (full casters + Warlock).
    val castsAtCreation = klass != null && CastingProgression.forClass(klass.id) in setOf(CastingProgression.FULL, CastingProgression.WARLOCK)
    // Above level 1, an Advancements step captures the subclass + each ASI-level choice earned so far.
    val advLevels = asiLevels(classId, level)
    val needsSubclass = klass != null && level >= klass.subclassLevel
    val needsAdvancements = klass != null && (needsSubclass || advLevels.isNotEmpty())
    val isSpellcaster = klass != null && CastingProgression.forClass(klass.id) != CastingProgression.NONE
    val steps = WizStep.entries.filter {
        (it != WizStep.FIGHTING_STYLE || grantsStyle) &&
            (it != WizStep.SPELLS || castsAtCreation) &&
            (it != WizStep.ADVANCEMENTS || needsAdvancements)
    }
    val step = steps[stepIndex.coerceIn(0, steps.lastIndex)]

    // First arrival on the Equipment step seeds the class + background starter kit.
    LaunchedEffect(step, equipmentSeeded) {
        if (step == WizStep.EQUIPMENT && !equipmentSeeded && classId != null) {
            inventory = StartingEquipment.seed(classId!!, backgroundId)
            equipmentSeeded = true
        }
    }

    fun canAdvance(): Boolean = when (step) {
        WizStep.NAME -> name.isNotBlank()
        WizStep.RACE -> race != null
        WizStep.CLASS -> klass != null
        WizStep.BACKGROUND -> background != null
        WizStep.ABILITIES -> true
        WizStep.ADVANCEMENTS -> (!needsSubclass || subclass != null) && advLevels.all { (advs[it] ?: AdvState()).isComplete() }
        WizStep.SKILLS -> klass != null && chosenSkills.size == klass.skillChoiceCount
        WizStep.FIGHTING_STYLE -> fightingStyle != null
        WizStep.EQUIPMENT -> true
        WizStep.SPELLS -> true
        WizStep.MANNER -> !alignment.isNullOrEmpty()
        WizStep.REVIEW -> true
    }

    fun build(): Character {
        val k = klass!!
        // Fold the advancement ASIs/half-feats into the scores (capped at 20) + collect chosen feats.
        var scores = finalScores
        val feats = mutableListOf<String>()
        if (needsAdvancements) {
            for (lvl in advLevels) {
                val choice = advs[lvl] ?: AdvState()
                for ((ability, inc) in choice.bumps()) scores = scores.with(ability, minOf(20, scores.score(ability) + inc))
                choice.chosenFeat?.let { feats += it }
            }
        }
        val conMod = AbilityScores.modifier(scores.constitution)
        val maxHp = LevelUpMath.firstLevelHp(k.hitDie, conMod) +
            LevelUpMath.averageGain(k.hitDie) * (level - 1)
        // Spell picks (A11): cantrips to cantripsKnown; 1st-level spells to the prepared or known list per class.
        val cantrips = if (castsAtCreation) chosenCantrips.toList() else emptyList()
        val spellList = if (castsAtCreation) chosenSpells.toList() else emptyList()
        val prepares = CastingProgression.usesPreparation(k.id)
        val payload = DnD5ePayload(
            race = raceId.orEmpty(),
            classes = listOf(ClassEntry(k.id, level, subclass = subclass.takeIf { needsSubclass })),
            abilityScores = scores,
            maxHp = maxHp,
            background = backgroundId.orEmpty(),
            selectedSkills = (backgroundSkills + chosenSkills).distinct(),
            alignment = alignment,
            personality = personality.trim(),
            ideals = ideals.trim(),
            bonds = bonds.trim(),
            flaws = flaws.trim(),
            backstory = backstory.trim(),
            inventory = inventory,
            fightingStyles = listOfNotNull(fightingStyle.takeIf { grantsStyle }),
            chosenFeats = feats.distinct(),
            cantripsKnown = cantrips,
            spellsKnown = if (spellList.isNotEmpty() && !prepares) mapOf(k.id to spellList) else emptyMap(),
            preparedSpells = if (spellList.isNotEmpty() && prepares) mapOf(k.id to spellList) else emptyMap(),
        ).withFullSpellSlots() // casters start the day with all slots
            .let { it.copy(currentHp = it.effectiveMaxHp) } // start at full incl. Tough / feat riders
        return if (existing == null) {
            Character.new(name.trim(), DnD5eRuleset(), payload, Instant.now())
        } else {
            existing.copy(name = name.trim(), payload = payload, updatedAt = Instant.now())
        }
    }

    fun jumpToReview() {
        editingFromReview = false
        stepIndex = steps.lastIndex
    }

    EditorShell(
        kicker = "Step ${stepOffset + stepIndex + 1} of ${stepOffset + steps.size}",
        title = if (existing == null) "New Character" else "Edit Character",
        stepCount = stepOffset + steps.size,
        currentIndex = stepOffset + stepIndex,
        onBack = {
            when {
                editingFromReview -> jumpToReview()
                stepIndex > 0 -> stepIndex--
                onExitFirstStep != null -> onExitFirstStep()
                else -> onCancel()
            }
        },
        onJump = { target ->
            editingFromReview = false
            if (target < stepOffset) {
                onExitFirstStep?.invoke()
            } else {
                stepIndex = (target - stepOffset).coerceIn(0, steps.lastIndex)
            }
        },
        scrollableContent = false, // step bodies scroll themselves
        footer = {
            if (editingFromReview && step != WizStep.REVIEW) {
                // Editing a single step from Review: one right-aligned Done back to Review.
                Spacer(Modifier.weight(1f))
                WizardPrimaryButton("Done", enabled = canAdvance()) { jumpToReview() }
            } else {
                WizardSecondaryButton("Cancel", onCancel)
                Spacer(Modifier.weight(1f))
                WizardPrimaryButton(
                    label = when {
                        step != WizStep.REVIEW -> "Continue"
                        existing == null -> "Create"
                        else -> "Save"
                    },
                    enabled = canAdvance(),
                ) { if (step == WizStep.REVIEW) onSave(build()) else stepIndex++ }
            }
        },
    ) {
        Column(Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                WizStep.NAME -> StepColumn {
                    IdentityStep(name, { name = it })
                    Spacer(Modifier.height(14.dp))
                    WizardFieldLabel("Starting level", hint = levelHint(level))
                    WizardLevelStepper("Character level", level) { level = it }
                }
                WizStep.RACE -> StepColumn {
                    WizardStepSection("Choose a Race", "Race grants ability bonuses, speed, vision, and starting traits.")
                    WizardChipsPicker(allRaces, { it.id == raceId }, { it.name }, large = true) { raceId = it.id }
                    Spacer(Modifier.height(10.dp))
                    NewRaceButton {
                        editingCustomRace = null
                        showRaceForm = true
                    }
                    race?.let { picked ->
                        RaceDetailCard(
                            race = picked,
                            isCustom = CustomRaceLibrary.isCustom(picked.id),
                            onEdit = {
                                editingCustomRace = picked
                                showRaceForm = true
                            },
                        )
                    }
                }
                WizStep.CLASS -> StepColumn {
                    Spacer(Modifier.height(18.dp))
                    CharacterLevelBanner(level)
                    WizardStepSection("Pick Class")
                    WizardChipsPicker(DnD5eCatalog.classes, { it.id == classId }, { it.name }, large = true) { picked ->
                        if (picked.id != classId) {
                            classId = picked.id
                            chosenSkills = emptySet()
                            fightingStyle = null
                            chosenCantrips = emptySet()
                            chosenSpells = emptySet()
                            subclass = null
                            advs = emptyMap()
                            // Reseed the starter kit for the new class unless editing a
                            // character that already carries real inventory.
                            if (source?.inventory?.isNotEmpty() != true) {
                                inventory = emptyList()
                                equipmentSeeded = false
                            }
                        }
                    }
                    klass?.let { ClassDetailCard(it, level) }
                }
                WizStep.BACKGROUND -> StepColumn {
                    WizardStepSection("Choose a Background", "Backgrounds grant a pair of skill proficiencies, gear, and a flavour feature.")
                    WizardChipsPicker(DnD5eCatalog.backgrounds, { it.id == backgroundId }, { it.name }, large = true) { backgroundId = it.id }
                    background?.let { BackgroundDetailCard(it) }
                }
                WizStep.ABILITIES -> AbilitiesStep(base, raceBonus, finalScores) { base = it }
                WizStep.ADVANCEMENTS -> AdvancementsStep(
                    klass, level, needsSubclass, advLevels, finalScores, isSpellcaster, subclass, advs,
                    onSubclass = { subclass = it },
                    onAdv = { lvl, st -> advs = advs + (lvl to st) },
                )
                WizStep.SKILLS -> SkillsStep(klass, background, backgroundSkills, chosenSkills) { chosenSkills = it }
                WizStep.FIGHTING_STYLE -> FightingStyleStep(fightingStyle) { fightingStyle = it }
                WizStep.EQUIPMENT -> EquipmentStep(
                    klass = klass,
                    inventory = inventory,
                    seeded = equipmentSeeded,
                    onChange = { inventory = it },
                    onReset = { inventory = StartingEquipment.seed(classId ?: "", backgroundId) },
                )
                WizStep.SPELLS -> SpellsStep(klass, chosenCantrips, chosenSpells, { chosenCantrips = it }, { chosenSpells = it })
                WizStep.MANNER -> MannerStep(
                    alignment, personality, ideals, bonds, flaws, backstory,
                    onAlignment = { alignment = it },
                    onPersonality = { personality = it },
                    onIdeals = { ideals = it },
                    onBonds = { bonds = it },
                    onFlaws = { flaws = it },
                    onBackstory = { backstory = it },
                )
                WizStep.REVIEW -> ReviewStep(
                    name = name, level = level, race = race, klass = klass, subclass = subclass,
                    background = background, scores = finalScores,
                    backgroundSkills = backgroundSkills, chosenSkills = chosenSkills,
                    inventory = inventory, castsAtCreation = castsAtCreation,
                    cantrips = chosenCantrips, spells = chosenSpells,
                    alignment = alignment, personality = personality, ideals = ideals,
                    bonds = bonds, flaws = flaws, backstory = backstory,
                    onEdit = { target ->
                        val index = steps.indexOf(target)
                        if (index >= 0) {
                            editingFromReview = true
                            stepIndex = index
                        }
                    },
                )
            }
        }
    }

    // Homebrew race form (parity #11): save updates-or-adds in the library and
    // selects the race; delete clears the wizard's selection if it matched.
    // Saved characters keep a dangling race id on delete (iOS behaviour).
    if (showRaceForm) {
        CustomRaceFormDialog(
            editing = editingCustomRace,
            onSave = { saved ->
                if (CustomRaceLibrary.races.value.any { it.id == saved.id }) {
                    CustomRaceLibrary.update(saved)
                } else {
                    CustomRaceLibrary.add(saved)
                }
                raceId = saved.id
                showRaceForm = false
            },
            onDelete = if (editingCustomRace == null) {
                null
            } else {
                { deletedId ->
                    CustomRaceLibrary.delete(deletedId)
                    if (raceId == deletedId) raceId = null
                    showRaceForm = false
                }
            },
            onDismiss = { showRaceForm = false },
        )
    }
}

// ── Shared step scaffolding ──────────────────────────────────────────────────

/** Every step body: a self-scrolling column in the shell's 22dp content well. */
@Composable
private fun StepColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, bottom = 16.dp),
        content = content,
    )
}

/** The iOS level-tier hint under the Name step's level stepper. */
private fun levelHint(level: Int): String = when (level) {
    1 -> "Most campaigns begin here."
    in 2..4 -> "Local heroes."
    in 5..10 -> "Heroes of the realm."
    in 11..16 -> "Masters of the realm."
    else -> "Masters of the world."
}

// ── Race ─────────────────────────────────────────────────────────────────────

@Composable
private fun RaceDetailCard(race: Race, isCustom: Boolean = false, onEdit: (() -> Unit)? = null) {
    val palette = MaterialTheme.natPalette
    Spacer(Modifier.height(12.dp))
    WizardDetailCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                race.name,
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                fontSize = 22.sp,
                color = palette.accent,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isCustom) {
                HomebrewBadge()
                Spacer(Modifier.weight(1f))
                if (onEdit != null) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(palette.tileStrong)
                            .border(1.dp, palette.accent.copy(alpha = 0.4f), CircleShape)
                            .clickable(onClick = onEdit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit homebrew race",
                            tint = palette.accent,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }
        if (race.description.isNotEmpty()) {
            Text(
                race.description,
                fontFamily = EbGaramond,
                fontSize = 13.sp,
                color = palette.inkSoft,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        WizardStatCell("Bonus", formatAsi(race.abilityBonuses()), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardStatCell("Size", race.size.replaceFirstChar { it.uppercase() }, modifier = Modifier.weight(1f))
            WizardStatCell("Speed", "${race.speed} ft", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardStatCell("Darkvision", if (race.darkvision > 0) "${race.darkvision} ft" else "None", modifier = Modifier.weight(1f))
            WizardStatCell("Age", race.typicalAge?.let { "~$it yrs" } ?: "—", modifier = Modifier.weight(1f))
        }
        if (race.languages.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            WizardStatCell("Languages", race.languages.joinToString(" · "), modifier = Modifier.fillMaxWidth())
        }
        race.traits.forEach { trait ->
            Spacer(Modifier.height(6.dp))
            WizardQuoteBlock(trait.name, trait.body)
        }
    }
}

private fun formatAsi(bonuses: Map<Ability, Int>): String =
    if (bonuses.isEmpty()) {
        "—"
    } else {
        Ability.entries.filter { it in bonuses }.joinToString(" · ") { "+${bonuses[it]} ${it.abbreviation}" }
    }

// ── Class ────────────────────────────────────────────────────────────────────

@Composable
private fun CharacterLevelBanner(level: Int) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(palette.accent.copy(alpha = 0.10f), Color.Transparent)))
            .border(1.2.dp, palette.accent.copy(alpha = 0.33f), RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "CHARACTER LEVEL",
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            color = palette.accent,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$level",
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = palette.accent,
        )
    }
}

@Composable
private fun ClassDetailCard(klass: CharacterClass, level: Int) {
    val palette = MaterialTheme.natPalette
    Spacer(Modifier.height(12.dp))
    WizardDetailCard {
        Text(
            klass.name,
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp,
            color = palette.accent,
        )
        if (klass.description.isNotEmpty()) {
            Text(
                klass.description,
                fontFamily = EbGaramond,
                fontSize = 13.sp,
                color = palette.inkSoft,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardStatCell("Hit Die", "d${klass.hitDie}", modifier = Modifier.weight(1f))
            WizardStatCell("Saves", klass.savingThrowAbilities().joinToString(" · ") { it.abbreviation }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        WizardStatCell(
            "Skills",
            "Pick ${klass.skillChoiceCount} on the Skills step",
            modifier = Modifier.fillMaxWidth(),
        )
        val features = klass.features.filter { it.level <= level }
        features.forEach { feature ->
            Spacer(Modifier.height(6.dp))
            WizardQuoteBlock("L${feature.level} · ${feature.name}", feature.body)
        }
    }
}

// ── Background ───────────────────────────────────────────────────────────────

@Composable
private fun BackgroundDetailCard(background: Background) {
    val palette = MaterialTheme.natPalette
    Spacer(Modifier.height(12.dp))
    WizardDetailCard {
        Text(
            background.name,
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp,
            color = palette.accent,
        )
        if (background.description.isNotEmpty()) {
            Text(
                background.description,
                fontFamily = EbGaramond,
                fontSize = 13.sp,
                color = palette.inkSoft,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        WizardStatCell(
            "Skills",
            background.grantedSkills.joinToString(" · ") { skillName(it) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        WizardStatCell(
            "Tools & Languages",
            toolsAndLanguages(background),
            modifier = Modifier.fillMaxWidth(),
        )
        if (background.equipment.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "EQUIPMENT",
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = palette.inkMute,
            )
            Spacer(Modifier.height(4.dp))
            background.equipment.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth()) {
                    pair.forEach { line ->
                        Row(
                            Modifier.weight(1f).padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Diamond(size = 4.dp, fill = palette.accent)
                            Text(line, fontFamily = Cormorant, fontSize = 13.sp, color = palette.ink)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        background.feature?.let { feature ->
            Spacer(Modifier.height(6.dp))
            WizardQuoteBlock(feature.name, feature.body)
        }
    }
}

private fun toolsAndLanguages(background: Background): String {
    val parts = mutableListOf<String>()
    if (background.toolProficiencies.isNotEmpty()) parts += background.toolProficiencies.joinToString(" · ")
    if (background.languageCount > 0) {
        parts += if (background.languageCount == 1) "1 language of your choice" else "${background.languageCount} languages of your choice"
    }
    return parts.joinToString(" · ").ifEmpty { "—" }
}

// ── Abilities ────────────────────────────────────────────────────────────────

@Composable
private fun AbilitiesStep(base: AbilityScores, raceBonus: Map<Ability, Int>, finalScores: AbilityScores, onChange: (AbilityScores) -> Unit) {
    val palette = MaterialTheme.natPalette
    StepColumn {
        WizardStepSection("Ability Scores", "Set base scores; racial bonuses are added automatically.")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Ability.entries.forEach { ability ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.tile)
                        .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        ability.abbreviation,
                        fontFamily = Cinzel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = palette.inkMute,
                        modifier = Modifier.width(38.dp),
                    )
                    Text(
                        abilityName(ability),
                        fontFamily = Cormorant,
                        fontSize = 15.sp,
                        color = palette.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${base.score(ability)}",
                        fontFamily = Cormorant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = palette.accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp),
                    )
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(palette.tileStrong)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        StepperButton("−", enabled = base.score(ability) > 3, size = 22) {
                            onChange(base.with(ability, base.score(ability) - 1))
                        }
                        StepperButton("+", enabled = base.score(ability) < 20, size = 22) {
                            onChange(base.with(ability, base.score(ability) + 1))
                        }
                    }
                }
            }
        }
        WizardStepSection("Final Scores", "With racial bonuses applied.")
        FinalScoresGrid(finalScores, raceBonus)
    }
}

@Composable
private fun FinalScoresGrid(scores: AbilityScores, raceBonus: Map<Ability, Int>) {
    val palette = MaterialTheme.natPalette
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Ability.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { ability ->
                    val bonus = raceBonus[ability] ?: 0
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(palette.tile)
                            .border(1.dp, palette.accent.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                ability.abbreviation,
                                fontFamily = Cinzel,
                                fontSize = 11.sp,
                                color = palette.inkMute,
                            )
                            if (bonus != 0) {
                                Text(
                                    if (bonus > 0) "+$bonus" else "$bonus",
                                    fontFamily = Cinzel,
                                    fontSize = 10.sp,
                                    color = palette.accentGold,
                                )
                            }
                        }
                        Text(
                            "${scores.score(ability)}",
                            fontFamily = Cormorant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                            color = palette.ink,
                        )
                        val mod = AbilityScores.modifier(scores.score(ability))
                        Text(
                            if (mod >= 0) "+$mod" else "$mod",
                            fontFamily = Cormorant,
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            color = palette.inkSoft,
                        )
                    }
                }
            }
        }
    }
}

private fun abilityName(ability: Ability): String =
    ability.name.lowercase().replaceFirstChar { it.uppercase() }

// ── Advancements ─────────────────────────────────────────────────────────────

/**
 * The above-level-1 creation step (A11): pick the subclass earned by [level] and,
 * for each Ability-Score-Improvement level reached, allocate a +2/one, +1/two, or
 * a prereq-filtered feat (half-feats get their +1). Mirrors the 2024 wizard's
 * Advancements step; reuses the same `LevelUp` machinery via the folded payload.
 */
@Composable
private fun AdvancementsStep(
    klass: CharacterClass?,
    level: Int,
    needsSubclass: Boolean,
    advLevels: List<Int>,
    scores: AbilityScores,
    isSpellcaster: Boolean,
    subclass: String?,
    advs: Map<Int, AdvState>,
    onSubclass: (String) -> Unit,
    onAdv: (Int, AdvState) -> Unit,
) {
    klass ?: return
    val palette = MaterialTheme.natPalette
    StepColumn {
        WizardStepSection(
            "Advancements",
            "Your ${klass.name} is level $level — make the choices earned along the way.",
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (needsSubclass && klass.subclasses.isNotEmpty()) {
                WizardSubSectionCard("Subclass", "level ${klass.subclassLevel}") {
                    Spacer(Modifier.height(8.dp))
                    WizardChipsPicker(klass.subclasses, { it.id == subclass }, { it.name }) { onSubclass(it.id) }
                }
            }
            advLevels.forEach { lvl ->
                val state = advs[lvl] ?: AdvState()
                WizardSubSectionCard("Level $lvl", "improvement or feat") {
                    Spacer(Modifier.height(8.dp))
                    WizardSegmented(
                        AdvKind.entries.map { it.label },
                        selectedIndex = AdvKind.entries.indexOf(state.kind),
                    ) { index -> onAdv(lvl, AdvState(AdvKind.entries[index])) }
                    Spacer(Modifier.height(8.dp))
                    when (state.kind) {
                        AdvKind.ASI_ONE, AdvKind.ASI_TWO -> {
                            val limit = if (state.kind == AdvKind.ASI_ONE) 1 else 2
                            WizardMultiSelectChips(
                                Ability.entries.toList(),
                                isSelected = { it in state.abilities },
                                label = { "${it.abbreviation} ${scores.score(it)}" },
                                quotaFull = state.abilities.size >= limit,
                                enabled = { scores.score(it) < 20 || it in state.abilities },
                            ) { a ->
                                onAdv(lvl, state.copy(abilities = if (a in state.abilities) state.abilities - a else state.abilities + a))
                            }
                        }
                        AdvKind.FEAT -> {
                            val available = Feats.available(scores, isSpellcaster)
                            WizardChipsPicker(available, { it.id == state.featId }, { it.name }) {
                                onAdv(lvl, state.copy(featId = it.id, half = null))
                            }
                            state.featId?.let { Feats.feat(it) }?.let { f ->
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
                                    WizardFieldLabel("+1 ability score")
                                    WizardChipsPicker(
                                        f.halfFeatAbilities.filter { scores.score(it) < 20 },
                                        { it == state.half },
                                        { "${it.abbreviation} ${scores.score(it)}" },
                                    ) { onAdv(lvl, state.copy(half = it)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Skills ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillsStep(
    klass: CharacterClass?,
    background: Background?,
    backgroundSkills: List<String>,
    chosen: Set<String>,
    onChange: (Set<String>) -> Unit,
) {
    val quota = klass?.skillChoiceCount ?: 0
    val options = klass?.skillChoiceFrom.orEmpty()
    StepColumn {
        if (backgroundSkills.isNotEmpty()) {
            WizardStepSection("From your background", background?.name)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                backgroundSkills.forEach { GrantedSkillChip(skillName(it)) }
            }
        }
        WizardStepSection(
            "From your class",
            "${klass?.name ?: "Class"} · pick $quota — ${chosen.size} / $quota chosen",
        )
        WizardMultiSelectChips(
            options,
            isSelected = { it in chosen },
            label = { id -> "${skillName(id)} (${DnD5eCatalog.skill(id)?.ability?.abbreviation ?: "?"})" },
            quotaFull = chosen.size >= quota,
            enabled = { it !in backgroundSkills },
        ) { id -> onChange(if (id in chosen) chosen - id else chosen + id) }
    }
}

// GrantedSkillChip now lives in WizardAtoms.kt (shared with the 2024 wizard).

// ── Fighting Style (Android-only step; iOS folds this elsewhere — see PARITY) ─

@Composable
private fun FightingStyleStep(selected: String?, onPick: (String) -> Unit) {
    StepColumn {
        WizardStepSection("Fighting Style", "Choose the style your class grants.")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FightingStyles.all.forEach { style ->
                SelectableRow(selected = selected == style.id, onClick = { onPick(style.id) }) {
                    NameWithMeta(style.name, style.description)
                }
            }
        }
    }
}

// ── Equipment ────────────────────────────────────────────────────────────────

/** ItemKind display order for the grouped list (spec order). */
private val KIND_ORDER = listOf(
    ItemKind.WEAPON, ItemKind.ARMOR, ItemKind.SHIELD, ItemKind.AMMUNITION,
    ItemKind.POTION, ItemKind.SCROLL, ItemKind.WONDROUS, ItemKind.TOOL,
    ItemKind.GEAR, ItemKind.TREASURE,
)

@Composable
private fun EquipmentStep(
    klass: CharacterClass?,
    inventory: List<InventoryItem>,
    seeded: Boolean,
    onChange: (List<InventoryItem>) -> Unit,
    onReset: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var showAdd by remember { mutableStateOf(false) }
    StepColumn {
        WizardStepSection(
            "Starting Kit",
            "${klass?.name ?: "Your class"}'s gear, plus anything you add by hand.",
        )
        Row(
            Modifier
                .clip(CircleShape)
                .background(palette.tileStrong)
                .border(1.2.dp, palette.accent.copy(alpha = 0.5f), CircleShape)
                .clickable { showAdd = true }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(14.dp))
            Text(
                "ADD ITEM",
                fontFamily = Cinzel,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = palette.accent,
            )
        }
        Spacer(Modifier.height(10.dp))
        KIND_ORDER.forEach { kind ->
            val group = inventory.filter { it.kind == kind }
            if (group.isEmpty()) return@forEach
            Text(
                "${kind.name} (${group.size})",
                fontFamily = Cinzel,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = palette.accent,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            group.forEach { item ->
                InventoryRow(
                    item = item,
                    onToggleEquip = { onChange(toggleEquip(inventory, item)) },
                )
            }
        }
        if (seeded) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Reset to starter kit",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = palette.accent,
                modifier = Modifier.clickable(onClick = onReset),
            )
        }
    }
    if (showAdd) {
        WizardAddItemDialog(
            onAdd = { onChange(inventory + it); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun InventoryRow(item: InventoryItem, onToggleEquip: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val equipToggle = item.kind == ItemKind.WEAPON || item.kind == ItemKind.ARMOR || item.kind == ItemKind.SHIELD
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(if (item.equipped) palette.accent.copy(alpha = 0.06f) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Diamond(size = 5.dp, fill = palette.accent)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name, fontFamily = Cormorant, fontSize = 15.sp, color = palette.ink)
                if (item.quantity > 1) {
                    Text("× ${item.quantity}", fontFamily = Cormorant, fontSize = 13.sp, color = palette.inkMute)
                }
            }
            item.weapon?.let { weapon ->
                Text(
                    "${weapon.damageDice} ${weapon.damageType}",
                    fontFamily = Cormorant,
                    fontSize = 12.sp,
                    color = palette.inkSoft,
                )
            }
        }
        if (equipToggle) {
            Box(Modifier.clip(CircleShape).clickable(onClick = onToggleEquip).padding(2.dp)) {
                WizardSelectionDot(selected = item.equipped)
            }
        }
    }
}

/** Equipping armor or a shield unequips its rivals; weapons toggle freely. */
private fun toggleEquip(inventory: List<InventoryItem>, target: InventoryItem): List<InventoryItem> =
    inventory.map { item ->
        when {
            item.id == target.id -> item.copy(equipped = !item.equipped)
            !target.equipped && item.kind == target.kind &&
                (target.kind == ItemKind.ARMOR || target.kind == ItemKind.SHIELD) -> item.copy(equipped = false)
            else -> item
        }
    }

private data class WizardCatalogueChoice(val name: String, val category: String, val make: (Int) -> InventoryItem)

/** Simple catalogue search + quantity stepper (full custom-item form is deferred). */
@Composable
private fun WizardAddItemDialog(onAdd: (InventoryItem) -> Unit, onDismiss: () -> Unit) {
    val choices = remember {
        buildList {
            DnD5eCatalog.weapons.forEach { w -> add(WizardCatalogueChoice(w.name, "Weapon") { qty -> w.makeItem(quantity = qty) }) }
            DnD5eCatalog.armor.forEach { a -> add(WizardCatalogueChoice(a.name, "Armor") { a.makeItem() }) }
            DnD5eCatalog.gear.forEach { g -> add(WizardCatalogueChoice(g.name, "Gear") { qty -> g.makeItem(quantity = qty) }) }
        }.sortedBy { it.name }
    }
    var query by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }
    val filtered = remember(query) {
        if (query.isBlank()) choices else choices.filter { it.name.contains(query, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quantity", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    StepperButton("−", enabled = quantity > 1, size = 28) { quantity-- }
                    Text("$quantity", style = MaterialTheme.typography.titleMedium)
                    StepperButton("+", enabled = quantity < 99, size = 28) { quantity++ }
                }
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(filtered, key = { it.name }) { choice ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onAdd(choice.make(quantity)) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(choice.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                choice.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Spells ───────────────────────────────────────────────────────────────────

@Composable
private fun SpellsStep(
    klass: CharacterClass?,
    cantrips: Set<String>,
    spells: Set<String>,
    onCantrips: (Set<String>) -> Unit,
    onSpells: (Set<String>) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    val className = klass?.name ?: return
    // Filter the SRD library to this class's cantrips and 1st-level spells.
    val cantripPool = remember(className) { DnD5eCatalog.spellLibrary.filter { it.level == 0 && it.classNames.any { c -> c.equals(className, ignoreCase = true) } }.sortedBy { it.name } }
    val spellPool = remember(className) { DnD5eCatalog.spellLibrary.filter { it.level == 1 && it.classNames.any { c -> c.equals(className, ignoreCase = true) } }.sortedBy { it.name } }
    StepColumn {
        WizardStepSection("Cantrips", "${cantrips.size} chosen")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            cantripPool.forEach { spell ->
                SpellRow(spell, selected = spell.index in cantrips) {
                    onCantrips(if (spell.index in cantrips) cantrips - spell.index else cantrips + spell.index)
                }
            }
        }
        WizardStepSection("1st-Level Spells", "${spells.size} chosen")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            spellPool.forEach { spell ->
                SpellRow(spell, selected = spell.index in spells) {
                    onSpells(if (spell.index in spells) spells - spell.index else spells + spell.index)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "You can add or change spells anytime on the Spells tab.",
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp,
            color = palette.inkMute,
        )
    }
}

@Composable
private fun SpellRow(spell: Spell, selected: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    SelectableRow(selected = selected, onClick = onClick) {
        Column(Modifier.weight(1f)) {
            Text(
                spell.name,
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = palette.ink,
            )
            Text(
                listOf(spell.schoolName, spell.castingTime, spell.range).filter { it.isNotEmpty() }.joinToString(" · "),
                fontFamily = Cormorant,
                fontSize = 12.sp,
                color = palette.inkSoft,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            spell.components.forEach { component ->
                Box(
                    Modifier
                        .size(16.dp)
                        .border(1.dp, palette.ink.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(component, fontFamily = Cinzel, fontSize = 8.sp, color = palette.inkSoft)
                }
            }
        }
    }
}

/** Shared selectable list row: selection dot + content, accent tint when picked. */
@Composable
private fun SelectableRow(selected: Boolean, onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) palette.accent.copy(alpha = 0.06f) else Color.Transparent)
            .border(
                if (selected) 1.4.dp else 1.dp,
                if (selected) palette.accent else palette.ink.copy(alpha = 0.13f),
                RoundedCornerShape(4.dp),
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WizardSelectionDot(selected)
        content()
    }
}

/** Name + smaller meta line, weighted to fill (fighting styles, simple rows). */
@Composable
private fun androidx.compose.foundation.layout.RowScope.NameWithMeta(name: String, meta: String) {
    val palette = MaterialTheme.natPalette
    Column(Modifier.weight(1f)) {
        Text(name, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.ink)
        Text(meta, fontFamily = Cormorant, fontSize = 12.sp, color = palette.inkSoft)
    }
}

// ── Manner ───────────────────────────────────────────────────────────────────

private val ALIGNMENT_GRID = listOf(
    listOf("Lawful Good", "Neutral Good", "Chaotic Good"),
    listOf("Lawful Neutral", "True Neutral", "Chaotic Neutral"),
    listOf("Lawful Evil", "Neutral Evil", "Chaotic Evil"),
)

@Composable
private fun MannerStep(
    alignment: String?,
    personality: String,
    ideals: String,
    bonds: String,
    flaws: String,
    backstory: String,
    onAlignment: (String?) -> Unit,
    onPersonality: (String) -> Unit,
    onIdeals: (String) -> Unit,
    onBonds: (String) -> Unit,
    onFlaws: (String) -> Unit,
    onBackstory: (String) -> Unit,
) {
    StepColumn {
        WizardStepSection("Alignment")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ALIGNMENT_GRID.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { label ->
                        AlignmentTile(
                            label = label,
                            selected = alignment == label,
                            onClick = { onAlignment(if (alignment == label) null else label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        WizardStepSection("Manner", "Optional, but characters with bonds and flaws are richer to play.")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                WizardFieldLabel("Personality")
                WizardTextField("I face problems head-on…", personality, onPersonality, multiline = true)
            }
            Column {
                WizardFieldLabel("Ideals")
                WizardTextField("What drives them?", ideals, onIdeals, multiline = true)
            }
            Column {
                WizardFieldLabel("Bonds")
                WizardTextField("Who or what do they hold dear?", bonds, onBonds, multiline = true)
            }
            Column {
                WizardFieldLabel("Flaws")
                WizardTextField("Where do they fall short?", flaws, onFlaws, multiline = true)
            }
        }
        WizardStepSection("Backstory", "How did they come to walk this road?")
        WizardTextField("Born under a blood-red moon…", backstory, onBackstory, multiline = true, lineLimit = 8)
    }
}

@Composable
private fun AlignmentTile(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.natPalette
    Column(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) palette.accent.copy(alpha = 0.1f) else palette.tile)
            .border(
                if (selected) 1.4.dp else 1.dp,
                if (selected) palette.accent else palette.ink.copy(alpha = 0.2f),
                RoundedCornerShape(4.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        label.split(" ").forEach { word ->
            Text(
                word.uppercase(),
                fontFamily = Cinzel,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp,
                letterSpacing = 1.8.sp,
                color = if (selected) palette.accent else palette.ink,
                maxLines = 1,
            )
        }
    }
}

// ── Review ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewStep(
    name: String,
    level: Int,
    race: Race?,
    klass: CharacterClass?,
    subclass: String?,
    background: Background?,
    scores: AbilityScores,
    backgroundSkills: List<String>,
    chosenSkills: Set<String>,
    inventory: List<InventoryItem>,
    castsAtCreation: Boolean,
    cantrips: Set<String>,
    spells: Set<String>,
    alignment: String?,
    personality: String,
    ideals: String,
    bonds: String,
    flaws: String,
    backstory: String,
    onEdit: (WizStep) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    StepColumn {
        Spacer(Modifier.height(18.dp))
        // Hero card: drop-cap initial + name + race + level line (roster style).
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReviewDropCap(name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?")
            Column(Modifier.weight(1f)) {
                Text(
                    name.trim().ifEmpty { "Unnamed" },
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 28.sp,
                    color = palette.accent,
                    maxLines = 1,
                )
                race?.let {
                    Text(it.name, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = palette.inkSoft, maxLines = 1)
                }
                Text(
                    "Level $level ${klass?.name ?: "—"}",
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = palette.inkSoft,
                    maxLines = 1,
                )
            }
        }

        ReviewSectionHeader("Identity") { onEdit(WizStep.NAME) }
        ReviewLine("Name", name.trim())
        ReviewLine("Level", "$level")

        ReviewSectionHeader("Race") { onEdit(WizStep.RACE) }
        ReviewLine("Race", race?.name.orEmpty())

        ReviewSectionHeader("Class") { onEdit(WizStep.CLASS) }
        ReviewLine("Class", klass?.name.orEmpty())
        subclass?.let { id ->
            ReviewLine("Subclass", klass?.subclasses?.firstOrNull { it.id == id }?.name ?: id)
        }

        ReviewSectionHeader("Background") { onEdit(WizStep.BACKGROUND) }
        ReviewLine("Background", background?.name.orEmpty())

        ReviewSectionHeader("Abilities") { onEdit(WizStep.ABILITIES) }
        FinalScoresGrid(scores, emptyMap())

        ReviewSectionHeader("Skills") { onEdit(WizStep.SKILLS) }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            backgroundSkills.forEach { GrantedSkillChip(skillName(it)) }
            chosenSkills.forEach { id ->
                WizardChip(label = skillName(id), selected = false, strongFill = true) {}
            }
        }
        if (backgroundSkills.isEmpty() && chosenSkills.isEmpty()) ReviewLine("Skills", "")

        ReviewSectionHeader("Equipment") { onEdit(WizStep.EQUIPMENT) }
        if (inventory.isEmpty()) {
            ReviewLine("Gear", "")
        } else {
            inventory.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Diamond(size = 4.dp, fill = palette.accent)
                    Text(
                        if (item.quantity > 1) "${item.name} × ${item.quantity}" else item.name,
                        fontFamily = Cormorant,
                        fontSize = 15.sp,
                        color = palette.ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.equipped) {
                        Text(
                            "equipped",
                            fontFamily = ImFell,
                            fontStyle = FontStyle.Italic,
                            fontSize = 11.sp,
                            color = palette.accentGold,
                        )
                    }
                }
            }
        }

        if (castsAtCreation) {
            ReviewSectionHeader("Spells") { onEdit(WizStep.SPELLS) }
            ReviewLine("Cantrips", cantrips.joinToString(" · ") { spellName(it) })
            ReviewLine("1st level", spells.joinToString(" · ") { spellName(it) })
        }

        ReviewSectionHeader("Manner") { onEdit(WizStep.MANNER) }
        ReviewLine("Alignment", alignment.orEmpty())
        ReviewLine("Personality", personality)
        ReviewLine("Ideals", ideals)
        ReviewLine("Bonds", bonds)
        ReviewLine("Flaws", flaws)
        ReviewLine("Backstory", backstory)
    }
}

// ReviewSectionHeader / ReviewLine / ReviewDropCap now live in WizardAtoms.kt
// (shared with the 2024 wizard's Review step).

// ── Shared helpers ───────────────────────────────────────────────────────────

private fun skillName(id: String): String = DnD5eCatalog.skill(id)?.name ?: id

private fun spellName(index: String): String =
    DnD5eCatalog.spellLibrary.firstOrNull { it.index == index }?.name ?: index

/** Base = stored final minus the (current race's) bonuses; defaults to all-10 for a new character. */
private fun initialBaseScores(source: DnD5ePayload?): AbilityScores {
    val scores = source?.abilityScores ?: AbilityScores()
    val bonus = source?.race?.let { DnD5eCatalog.race(it)?.abilityBonuses() }.orEmpty()
    return Ability.entries.fold(scores) { acc, ability ->
        acc.with(ability, max(1, acc.score(ability) - (bonus[ability] ?: 0)))
    }
}

/** Class-chosen skills = stored proficiencies minus the ones the background granted. */
private fun classChosenSkills(source: DnD5ePayload?): Set<String> {
    val granted = source?.background?.let { DnD5eCatalog.background(it)?.grantedSkills }.orEmpty()
    return source?.selectedSkills.orEmpty().filterNot { it in granted }.toSet()
}

private fun AbilityScores.applying(bonus: Map<Ability, Int>): AbilityScores =
    Ability.entries.fold(this) { acc, ability -> acc.with(ability, acc.score(ability) + (bonus[ability] ?: 0)) }
