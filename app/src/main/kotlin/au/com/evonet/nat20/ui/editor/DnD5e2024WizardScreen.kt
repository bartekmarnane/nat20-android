package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.dnd5e2024.Armor2024
import au.com.evonet.nat20.dnd5e2024.Armors2024
import au.com.evonet.nat20.dnd5e2024.Background2024
import au.com.evonet.nat20.dnd5e2024.CharacterClass2024
import au.com.evonet.nat20.dnd5e2024.ClassEntry2024
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Catalog
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Ruleset
import au.com.evonet.nat20.dnd5e2024.FeatCategory2024
import au.com.evonet.nat20.dnd5e2024.Feats2024
import au.com.evonet.nat20.dnd5e2024.FightingStyle2024
import au.com.evonet.nat20.dnd5e2024.Species2024
import au.com.evonet.nat20.dnd5e2024.Spell2024
import au.com.evonet.nat20.dnd5e2024.Spellcasting2024Creation
import au.com.evonet.nat20.dnd5e2024.Weapon2024
import au.com.evonet.nat20.dnd5e2024.Weapons2024
import au.com.evonet.nat20.dnd5e2024.WeaponMasteryProgression2024
import au.com.evonet.nat20.dnd5e2024.armorClass
import au.com.evonet.nat20.dnd5e2024.effectiveMaxHp
import au.com.evonet.nat20.dnd5e2024.withFullSpellSlots
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette
import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * The D&D 5e (2024) creation wizard (A21, restyled to iOS parity): Name →
 * Species → Class → Subclass (level 3+) → Background → Skills → Abilities →
 * Advancements (ASI levels reached) → Weapon Mastery (martial classes) →
 * Fighting Style (Fighter 1 / Paladin·Ranger 2) → Spells (casters) → Armor →
 * Review. The 2024 differences are here — **species carry no ability bumps**
 * (traits only) and **the background grants the +2/+1 ASI** (allocated on the
 * Background step) plus an origin feat. Hosted in the shared [EditorShell]
 * chrome on the shared wizard atoms; the unified creation flow prefixes the
 * Ruleset step (`stepOffset = 1`).
 */
private enum class Wiz2024(val title: String) {
    NAME("Name"), SPECIES("Species"), CLASS("Class"), SUBCLASS("Subclass"),
    BACKGROUND("Background"), SKILLS("Skills"), ABILITIES("Abilities"),
    ADVANCEMENTS("Advancements"), WEAPON_MASTERY("Weapon Mastery"),
    FIGHTING_STYLE("Fighting Style"), SPELLS("Spells"), ARMOR("Armor"),
    REVIEW("Review"),
}

/** A single ASI-level choice in the above-level-1 creation step: +2 of bumps, or a General feat. */
private enum class AdvKind2024(val label: String) { ASI("Ability Scores"), FEAT("Feat") }

@Serializable
private data class AdvState2024(
    val kind: AdvKind2024 = AdvKind2024.ASI,
    /** ASI mode: per-ability deltas that must total +2 (a +2/one or +1/two). */
    val bumps: Map<Ability, Int> = emptyMap(),
    val featId: String? = null,
    val half: Ability? = null,
) {
    /** The ability bumps this choice contributes (the ASI deltas, or a half-feat's +1). */
    fun abilityBumps(): Map<Ability, Int> = when (kind) {
        AdvKind2024.ASI -> bumps
        AdvKind2024.FEAT -> half?.let { mapOf(it to 1) }.orEmpty()
    }

    val chosenFeat: String? get() = featId.takeIf { kind == AdvKind2024.FEAT }

    fun isComplete(): Boolean = when (kind) {
        AdvKind2024.ASI -> bumps.values.sum() == 2 && bumps.values.all { it in 1..2 }
        AdvKind2024.FEAT -> featId != null && (Feats2024.feat(featId)?.grantsAbilityIncrease != true || half != null)
    }
}

/** Levels at or below [level] where [classId] grants an Ability Score Improvement. */
private fun asiLevels2024(classId: String?, level: Int): List<Int> =
    if (classId == null) emptyList() else (1..level).filter { LevelUpMath.grantsAbilityScoreImprovement(classId, it) }

@Composable
fun DnD5e2024WizardScreen(
    onSave: (Character) -> Unit,
    onCancel: () -> Unit,
    stepOffset: Int = 0,
    onExitFirstStep: (() -> Unit)? = null,
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var speciesId by rememberSaveable { mutableStateOf<String?>(null) }
    var classId by rememberSaveable { mutableStateOf<String?>(null) }
    var level by rememberSaveable { mutableIntStateOf(1) }
    var backgroundId by rememberSaveable { mutableStateOf<String?>(null) }
    var base by rememberSaveable(stateSaver = jsonStateSaver<AbilityScores>()) { mutableStateOf(AbilityScores()) }
    /** The background's +2/+1 (or +1/+1/+1) allocation across its three ability options. */
    var increases by rememberSaveable(stateSaver = jsonStateSaver<Map<Ability, Int>>()) { mutableStateOf(emptyMap()) }
    var chosenSkills by rememberSaveable(stateSaver = jsonStateSaver<Set<String>>()) { mutableStateOf(emptySet()) }
    var subclass by rememberSaveable { mutableStateOf<String?>(null) }
    var advs by rememberSaveable(stateSaver = jsonStateSaver<Map<Int, AdvState2024>>()) { mutableStateOf(emptyMap()) }
    var masteries by rememberSaveable(stateSaver = jsonStateSaver<Set<String>>()) { mutableStateOf(emptySet()) }
    var fightingStyle by rememberSaveable { mutableStateOf<String?>(null) }
    var chosenCantrips by rememberSaveable(stateSaver = jsonStateSaver<Set<String>>()) { mutableStateOf(emptySet()) }
    var chosenSpells by rememberSaveable(stateSaver = jsonStateSaver<Set<String>>()) { mutableStateOf(emptySet()) }
    var armorId by rememberSaveable { mutableStateOf<String?>(null) }
    var hasShield by rememberSaveable { mutableStateOf(false) }
    // Pencil-jump from Review: the footer collapses to a single "Done" back to Review.
    var editingFromReview by rememberSaveable { mutableStateOf(false) }

    val species = speciesId?.let(DnD5e2024Catalog::species)
    val klass = classId?.let(DnD5e2024Catalog::characterClass)
    val background = backgroundId?.let(DnD5e2024Catalog::background)
    val backgroundSkills = background?.skills.orEmpty()
    val allocationValid = background != null && increasesValid(increases, background.abilityOptions)
    val finalScores = base.applying(increases)

    val asiLevels = asiLevels2024(classId, level)
    val needsSubclass = klass != null && level >= klass.subclassLevel && klass.subclasses.isNotEmpty()
    val masterySlots = klass?.let { WeaponMasteryProgression2024.slots(it.id, level) } ?: 0
    val styleGrantLevel = klass?.let { FightingStyle2024.grantLevel(it.id) }
    val grantsStyle = styleGrantLevel != null && level >= styleGrantLevel

    // Spell pools + quotas (presentation capped at what the catalogue offers).
    val cantripPool = remember(classId) { spellPool2024(classId, spellLevel = 0) }
    val leveledPool = remember(classId) { spellPool2024(classId, spellLevel = 1) }
    val cantripCap = klass?.let { minOf(Spellcasting2024Creation.cantripsKnown(it.id, level), cantripPool.size) } ?: 0
    val leveledCap = klass?.let { minOf(Spellcasting2024Creation.leveledSpellsKnown(it.id, level), leveledPool.size) } ?: 0
    val hasSpellChoices = klass?.isCaster == true && cantripCap + leveledCap > 0

    val steps = Wiz2024.entries.filter {
        when (it) {
            Wiz2024.SUBCLASS -> needsSubclass
            Wiz2024.SKILLS -> klass == null || klass.skillChoiceCount > 0
            Wiz2024.ADVANCEMENTS -> asiLevels.isNotEmpty()
            Wiz2024.WEAPON_MASTERY -> masterySlots > 0
            Wiz2024.FIGHTING_STYLE -> grantsStyle
            Wiz2024.SPELLS -> hasSpellChoices
            else -> true
        }
    }
    val step = steps[stepIndex.coerceIn(0, steps.lastIndex)]

    fun canAdvance(): Boolean = when (step) {
        Wiz2024.NAME -> name.isNotBlank()
        Wiz2024.SPECIES -> species != null
        Wiz2024.CLASS -> klass != null
        Wiz2024.SUBCLASS -> subclass != null
        Wiz2024.BACKGROUND -> background != null && allocationValid
        Wiz2024.SKILLS -> klass != null && chosenSkills.size == klass.skillChoiceCount
        Wiz2024.ABILITIES -> true
        Wiz2024.ADVANCEMENTS -> asiLevels.all { (advs[it] ?: AdvState2024()).isComplete() }
        Wiz2024.WEAPON_MASTERY -> masteries.size == masterySlots
        Wiz2024.FIGHTING_STYLE -> fightingStyle != null
        Wiz2024.SPELLS -> true
        Wiz2024.ARMOR -> true
        Wiz2024.REVIEW -> true
    }

    /** Background scores plus every advancement's ASI/half-feat bumps (capped 20) + the feats taken. */
    fun foldAdvancements(): Pair<AbilityScores, List<String>> {
        var scores = finalScores
        val feats = mutableListOf<String>()
        for (lvl in asiLevels) {
            val choice = advs[lvl] ?: AdvState2024()
            for ((ability, inc) in choice.abilityBumps()) scores = scores.with(ability, minOf(20, scores.score(ability) + inc))
            choice.chosenFeat?.let { feats += it }
        }
        return scores to feats.distinct()
    }

    val (foldedScores, advFeats) = foldAdvancements()
    val validStyle = fightingStyle.takeIf { id ->
        grantsStyle && id != null && Feats2024.feat(id)?.category == FeatCategory2024.FIGHTING_STYLE
    }
    val armorProficient = armorId?.let { id ->
        val worn = Armors2024.armor(id)
        klass != null && worn != null && worn.category in klass.armorProficiencies
    } == true

    fun build(): Character {
        val k = klass!!
        val conMod = AbilityScores.modifier(foldedScores.constitution)
        val maxHp = LevelUpMath.firstLevelHp(k.hitDie, conMod) + LevelUpMath.averageGain(k.hitDie) * (level - 1)
        // Fold selections defensively: dedup + cap masteries, drop picks the catalogue no longer offers.
        val masteryList = Weapons2024.all.filter { it.id in masteries }.map { it.id }.take(masterySlots)
        val cantrips = cantripPool.filter { it.id in chosenCantrips }.map { it.id }.take(cantripCap)
        val leveled = leveledPool.filter { it.id in chosenSpells }.map { it.id }.take(leveledCap)
        val payload = DnD5e2024Payload(
            species = speciesId.orEmpty(),
            classes = listOf(ClassEntry2024(k.id, level, subclass = subclass.takeIf { needsSubclass })),
            background = backgroundId.orEmpty(),
            abilityScores = foldedScores,
            baseAbilityScores = base,
            backgroundASI = increases,
            maxHp = maxHp,
            skillProficiencies = (backgroundSkills + chosenSkills).distinct(),
            originFeat = background?.originFeat,
            chosenFeats = advFeats,
            fightingStyle = validStyle,
            weaponMasteries = masteryList,
            cantripsKnown = cantrips,
            preparedSpells = if (leveled.isNotEmpty()) mapOf(k.id to leveled) else emptyMap(),
            equippedArmor = armorId.takeIf { armorProficient },
            hasShield = hasShield && k.hasShieldProficiency,
        ).withFullSpellSlots().let { it.copy(currentHp = it.effectiveMaxHp) } // start at full incl. Tough / Dwarven Toughness
        return Character.new(name.trim(), DnD5e2024Ruleset(), payload, Instant.now())
    }

    fun jumpToReview() {
        editingFromReview = false
        stepIndex = steps.lastIndex
    }

    EditorShell(
        kicker = "Step ${stepOffset + stepIndex + 1} of ${stepOffset + steps.size}",
        title = "New Character",
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
            if (editingFromReview && step != Wiz2024.REVIEW) {
                Spacer(Modifier.weight(1f))
                WizardPrimaryButton("Done", enabled = canAdvance()) { jumpToReview() }
            } else {
                WizardSecondaryButton("Cancel", onCancel)
                Spacer(Modifier.weight(1f))
                WizardPrimaryButton(
                    label = if (step == Wiz2024.REVIEW) "Create" else "Continue",
                    enabled = canAdvance(),
                ) { if (step == Wiz2024.REVIEW) onSave(build()) else stepIndex++ }
            }
        },
    ) {
        Column(Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                Wiz2024.NAME -> StepColumn2024 {
                    IdentityStep(name, { name = it })
                    Spacer(Modifier.height(14.dp))
                    WizardFieldLabel("Starting level", hint = levelHint2024(level))
                    WizardLevelStepper("Character level", level) { level = it }
                }
                Wiz2024.SPECIES -> StepColumn2024 {
                    WizardStepSection("Choose a Species", "In the 2024 rules a species grants traits only — ability bumps come from your background.")
                    WizardChipsPicker(DnD5e2024Catalog.species, { it.id == speciesId }, { it.name }, large = true) { speciesId = it.id }
                    species?.let { SpeciesDetailCard(it) }
                }
                Wiz2024.CLASS -> StepColumn2024 {
                    Spacer(Modifier.height(18.dp))
                    Banner2024("Character level", "$level")
                    WizardStepSection("Pick Class")
                    WizardChipsPicker(DnD5e2024Catalog.classes, { it.id == classId }, { it.name }, large = true) { picked ->
                        if (picked.id != classId) {
                            // iOS setClass rule: switching class clears every class-derived pick.
                            classId = picked.id
                            subclass = null
                            masteries = emptySet()
                            fightingStyle = null
                            chosenCantrips = emptySet()
                            chosenSpells = emptySet()
                            chosenSkills = emptySet()
                            armorId = null
                            hasShield = false
                        }
                    }
                    klass?.let { ClassDetailCard2024(it) }
                }
                Wiz2024.SUBCLASS -> SubclassStep2024(klass, subclass) { subclass = it }
                Wiz2024.BACKGROUND -> BackgroundStep2024(
                    background = background,
                    increases = increases,
                    onPick = { picked ->
                        if (picked.id != backgroundId) {
                            backgroundId = picked.id
                            // Seed the default +2 first option / +1 second; re-tapping keeps a custom allocation.
                            increases = defaultIncreases(picked)
                        }
                    },
                    onIncreases = { increases = it },
                )
                Wiz2024.SKILLS -> SkillsStep2024(klass, background, backgroundSkills, chosenSkills) { chosenSkills = it }
                Wiz2024.ABILITIES -> AbilitiesStep2024(base, increases, finalScores) { base = it }
                Wiz2024.ADVANCEMENTS -> AdvancementsStep2024(
                    klass, level, asiLevels, finalScores, advs,
                    onAdv = { lvl, state -> advs = advs + (lvl to state) },
                )
                Wiz2024.WEAPON_MASTERY -> WeaponMasteryStep2024(masterySlots, masteries) { masteries = it }
                Wiz2024.FIGHTING_STYLE -> FightingStyleStep2024(fightingStyle) { fightingStyle = it }
                Wiz2024.SPELLS -> SpellsStep2024(
                    cantripPool, leveledPool, cantripCap, leveledCap, chosenCantrips, chosenSpells,
                    onCantrips = { chosenCantrips = it },
                    onSpells = { chosenSpells = it },
                )
                Wiz2024.ARMOR -> ArmorStep2024(
                    klass = klass,
                    armorId = armorId,
                    hasShield = hasShield,
                    previewAC = DnD5e2024Payload(
                        species = speciesId.orEmpty(),
                        classes = listOfNotNull(klass?.let { ClassEntry2024(it.id, level, subclass) }),
                        background = backgroundId.orEmpty(),
                        abilityScores = foldedScores,
                        originFeat = background?.originFeat,
                        chosenFeats = advFeats,
                        fightingStyle = validStyle,
                        equippedArmor = armorId.takeIf { armorProficient },
                        hasShield = hasShield && klass?.hasShieldProficiency == true,
                    ).armorClass,
                    onArmor = { armorId = it },
                    onShield = { hasShield = it },
                )
                Wiz2024.REVIEW -> ReviewStep2024(
                    name = name, level = level, species = species, klass = klass,
                    subclass = subclass.takeIf { needsSubclass }, background = background,
                    scores = foldedScores, increases = increases, advFeats = advFeats,
                    backgroundSkills = backgroundSkills, chosenSkills = chosenSkills,
                    armorId = armorId.takeIf { armorProficient },
                    hasShield = hasShield && klass?.hasShieldProficiency == true,
                    fightingStyle = validStyle,
                    masteries = Weapons2024.all.filter { it.id in masteries }.map { it.id }.take(masterySlots),
                    cantrips = cantripPool.filter { it.id in chosenCantrips },
                    spells = leveledPool.filter { it.id in chosenSpells },
                    onEdit = { target ->
                        val index = steps.indexOf(target)
                        if (index >= 0) {
                            editingFromReview = true
                            stepIndex = index
                        }
                    },
                    isActive = { it in steps },
                )
            }
        }
    }
}

// ── Shared step scaffolding ──────────────────────────────────────────────────

/** Every step body: a self-scrolling column in the shell's 22dp content well. */
@Composable
private fun StepColumn2024(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, bottom = 16.dp),
        content = content,
    )
}

/** The iOS level-tier hint under the Name step's level stepper. */
private fun levelHint2024(level: Int): String = when (level) {
    1 -> "Most campaigns begin here."
    in 2..4 -> "Local heroes."
    in 5..10 -> "Heroes of the realm."
    in 11..16 -> "Masters of the realm."
    else -> "Masters of the world."
}

/** Accent banner row: Cinzel label + big Cormorant value (Character Level / Armor Class). */
@Composable
private fun Banner2024(label: String, value: String) {
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
            label.uppercase(),
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            color = palette.accent,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = palette.accent,
        )
    }
}

/**
 * The iOS `SelectableRow2024`: selection dot + title + optional subtitle/detail
 * lines; selected rows get the accent tint + stroke, rows at an unmet quota cap
 * dim and disable.
 */
@Composable
private fun SelectableRow2024(
    title: String,
    subtitle: String? = null,
    detail: String? = null,
    selected: Boolean,
    dimmed: Boolean = false,
    dimmedAlpha: Float = 0.5f,
    onClick: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (dimmed) dimmedAlpha else 1f)
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) palette.accent.copy(alpha = 0.06f) else Color.Transparent)
            .border(
                if (selected) 1.4.dp else 1.dp,
                if (selected) palette.accent else palette.ink.copy(alpha = 0.13f),
                RoundedCornerShape(4.dp),
            )
            .clickable(enabled = !dimmed, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WizardSelectionDot(selected)
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.ink)
            if (subtitle != null) {
                Text(subtitle, fontFamily = Cormorant, fontSize = 13.sp, color = palette.inkSoft)
            }
            if (detail != null) {
                Text(detail, fontFamily = Cormorant, fontSize = 12.sp, color = palette.inkSoft)
            }
        }
    }
}

// ── Species ──────────────────────────────────────────────────────────────────

@Composable
private fun SpeciesDetailCard(species: Species2024) {
    val palette = MaterialTheme.natPalette
    Spacer(Modifier.height(12.dp))
    WizardDetailCard {
        Text(
            species.name,
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp,
            color = palette.accent,
        )
        if (species.description.isNotEmpty()) {
            Text(
                species.description,
                fontFamily = EbGaramond,
                fontSize = 13.sp,
                color = palette.inkSoft,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardStatCell("Size", species.size, modifier = Modifier.weight(1f))
            WizardStatCell("Speed", "${species.speed} ft", modifier = Modifier.weight(1f))
        }
        if (species.darkvision > 0) {
            Spacer(Modifier.height(6.dp))
            WizardStatCell("Darkvision", "${species.darkvision} ft", modifier = Modifier.fillMaxWidth())
        }
        species.traits.forEach { trait ->
            Spacer(Modifier.height(6.dp))
            WizardQuoteBlock(trait, "")
        }
    }
}

// ── Class ────────────────────────────────────────────────────────────────────

@Composable
private fun ClassDetailCard2024(klass: CharacterClass2024) {
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
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardStatCell("Hit Die", "d${klass.hitDie}", modifier = Modifier.weight(1f))
            WizardStatCell("Saves", klass.savingThrows.joinToString(" · ") { it.abbreviation }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardStatCell("Skills", "Pick ${klass.skillChoiceCount}", modifier = Modifier.weight(1f))
            WizardStatCell("Subclass", "At level ${klass.subclassLevel}", modifier = Modifier.weight(1f))
        }
    }
}

// ── Subclass ─────────────────────────────────────────────────────────────────

@Composable
private fun SubclassStep2024(klass: CharacterClass2024?, subclass: String?, onPick: (String) -> Unit) {
    klass ?: return
    val palette = MaterialTheme.natPalette
    StepColumn2024 {
        WizardStepSection(
            "Choose a Subclass",
            "Every 2024 class commits to its subclass at level ${klass.subclassLevel}.",
        )
        WizardChipsPicker(klass.subclasses, { it.id == subclass }, { it.name }, large = true) { onPick(it.id) }
        klass.subclasses.firstOrNull { it.id == subclass }?.let { picked ->
            Spacer(Modifier.height(12.dp))
            WizardDetailCard {
                Text(
                    picked.name,
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 22.sp,
                    color = palette.accent,
                )
                Text(
                    "${klass.name} · chosen at level ${klass.subclassLevel}",
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = palette.inkSoft,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

// ── Background (picker + ASI allocator + origin feat) ────────────────────────

/** Valid iff every key is one of the background's options, each bump is +1..+2, and they total +3. */
private fun increasesValid(increases: Map<Ability, Int>, options: List<Ability>): Boolean =
    increases.keys.all { it in options } &&
        increases.values.all { it in 1..2 } &&
        increases.values.sum() == 3

/** The default allocation a fresh background pick seeds: +2 first option, +1 second. */
private fun defaultIncreases(background: Background2024): Map<Ability, Int> = buildMap {
    background.abilityOptions.getOrNull(0)?.let { put(it, 2) }
    background.abilityOptions.getOrNull(1)?.let { put(it, 1) }
}

@Composable
private fun BackgroundStep2024(
    background: Background2024?,
    increases: Map<Ability, Int>,
    onPick: (Background2024) -> Unit,
    onIncreases: (Map<Ability, Int>) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    StepColumn2024 {
        WizardStepSection(
            "Choose a Background",
            "Backgrounds grant the +2/+1 ability increases, two skills, and an origin feat.",
        )
        WizardChipsPicker(DnD5e2024Catalog.backgrounds, { it.id == background?.id }, { it.name }, large = true, onPick = onPick)
        if (background != null) {
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
                    background.skills.joinToString(" · ") { skill2024(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))
            val allocated = increases.values.sum()
            WizardSubSectionCard("Ability Score Increase", "($allocated/3)") {
                Text(
                    "Allocate +3 across the background's abilities — a +2 and a +1, or +1 to each.",
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    color = palette.inkMute,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    background.abilityOptions.forEach { ability ->
                        val value = increases[ability] ?: 0
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.tileStrong)
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
                                abilityName2024(ability),
                                fontFamily = Cormorant,
                                fontSize = 15.sp,
                                color = palette.ink,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (value > 0) "+$value" else "—",
                                fontFamily = Cormorant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp,
                                color = if (value > 0) palette.accent else palette.inkMute,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(40.dp),
                            )
                            Row(
                                Modifier.clip(CircleShape).background(palette.tile).padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                StepperButton("−", enabled = value > 0, size = 22) {
                                    onIncreases((increases + (ability to value - 1)).filterValues { it > 0 })
                                }
                                StepperButton("+", enabled = value < 2 && allocated < 3, size = 22) {
                                    onIncreases(increases + (ability to value + 1))
                                }
                            }
                        }
                    }
                }
            }
            Feats2024.feat(background.originFeat)?.let { feat ->
                Spacer(Modifier.height(10.dp))
                WizardSubSectionCard("Origin Feat") {
                    Spacer(Modifier.height(8.dp))
                    WizardQuoteBlock(feat.name, feat.description)
                }
            }
        }
    }
}

// ── Skills ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillsStep2024(
    klass: CharacterClass2024?,
    background: Background2024?,
    backgroundSkills: List<String>,
    chosen: Set<String>,
    onChange: (Set<String>) -> Unit,
) {
    val quota = klass?.skillChoiceCount ?: 0
    val options = klass?.skillChoiceFrom.orEmpty().filterNot { it in backgroundSkills }
    StepColumn2024 {
        if (backgroundSkills.isNotEmpty()) {
            WizardStepSection("From your background", background?.name)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                backgroundSkills.forEach { GrantedSkillChip(skill2024(it)) }
            }
        }
        WizardStepSection(
            "From your class",
            "${klass?.name ?: "Class"} · pick $quota — ${chosen.size} / $quota chosen",
        )
        WizardMultiSelectChips(
            options,
            isSelected = { it in chosen },
            label = { id -> "${skill2024(id)} (${DnD5eCatalog.skill(id)?.ability?.abbreviation ?: "?"})" },
            quotaFull = chosen.size >= quota,
        ) { id -> onChange(if (id in chosen) chosen - id else chosen + id) }
    }
}

// ── Abilities ────────────────────────────────────────────────────────────────

@Composable
private fun AbilitiesStep2024(
    base: AbilityScores,
    increases: Map<Ability, Int>,
    finalScores: AbilityScores,
    onChange: (AbilityScores) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    StepColumn2024 {
        WizardStepSection("Ability Scores", "Set base scores; your background's increases are added automatically.")
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
                        abilityName2024(ability),
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
        WizardStepSection("Final Scores", "With the background increases applied (max 20).")
        FinalScoresGrid2024(finalScores, increases)
    }
}

/** The final-scores grid: score + modifier per ability, accentGold badge for the background bump. */
@Composable
private fun FinalScoresGrid2024(scores: AbilityScores, bonus: Map<Ability, Int>) {
    val palette = MaterialTheme.natPalette
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Ability.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { ability ->
                    val bump = bonus[ability] ?: 0
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
                            if (bump != 0) {
                                Text(
                                    "+$bump",
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

private fun abilityName2024(ability: Ability): String =
    ability.name.lowercase().replaceFirstChar { it.uppercase() }

// ── Advancements (ASI-or-feat per earned level; subclass has its own step) ──

@Composable
private fun AdvancementsStep2024(
    klass: CharacterClass2024?,
    level: Int,
    asiLevels: List<Int>,
    scores: AbilityScores,
    advs: Map<Int, AdvState2024>,
    onAdv: (Int, AdvState2024) -> Unit,
) {
    klass ?: return
    val palette = MaterialTheme.natPalette
    StepColumn2024 {
        WizardStepSection(
            "Advancements",
            "Your ${klass.name} is level $level — make the improvement-or-feat choices earned along the way.",
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            asiLevels.forEach { lvl ->
                val state = advs[lvl] ?: AdvState2024()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.tile)
                        .border(1.dp, palette.ink.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(10.dp),
                ) {
                    Text(
                        "LEVEL $lvl",
                        fontFamily = Cinzel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 3.sp,
                        color = palette.accent,
                    )
                    Spacer(Modifier.height(8.dp))
                    WizardSegmented(
                        AdvKind2024.entries.map { it.label },
                        selectedIndex = AdvKind2024.entries.indexOf(state.kind),
                    ) { index -> onAdv(lvl, AdvState2024(AdvKind2024.entries[index])) }
                    Spacer(Modifier.height(8.dp))
                    when (state.kind) {
                        AdvKind2024.ASI -> {
                            val total = state.bumps.values.sum()
                            Text(
                                "Allocate +2 — a +2 to one ability or +1 to two. ($total/2)",
                                fontFamily = ImFell,
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp,
                                color = palette.inkMute,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Ability.entries.forEach { ability ->
                                    val value = state.bumps[ability] ?: 0
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "${ability.abbreviation} ${scores.score(ability)}",
                                            fontFamily = Cormorant,
                                            fontSize = 15.sp,
                                            color = palette.ink,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            if (value > 0) "+$value" else "—",
                                            fontFamily = Cormorant,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 17.sp,
                                            color = if (value > 0) palette.accent else palette.inkMute,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(36.dp),
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            StepperButton("−", enabled = value > 0, size = 22) {
                                                onAdv(lvl, state.copy(bumps = (state.bumps + (ability to value - 1)).filterValues { it > 0 }))
                                            }
                                            StepperButton(
                                                "+",
                                                enabled = value < 2 && total < 2 && scores.score(ability) + value < 20,
                                                size = 22,
                                            ) {
                                                onAdv(lvl, state.copy(bumps = state.bumps + (ability to value + 1)))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        AdvKind2024.FEAT -> {
                            val available = Feats2024.inCategory(FeatCategory2024.GENERAL)
                                .filter { it.id != "ability-score-improvement" && it.isAvailable(lvl, scores, klass.isCaster) }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                available.forEach { feat ->
                                    SelectableRow2024(
                                        title = feat.name,
                                        subtitle = feat.description,
                                        selected = state.featId == feat.id,
                                    ) { onAdv(lvl, state.copy(featId = feat.id, half = null)) }
                                }
                            }
                            state.featId?.let { Feats2024.feat(it) }?.takeIf { it.grantsAbilityIncrease }?.let {
                                Spacer(Modifier.height(6.dp))
                                WizardFieldLabel("+1 ability score")
                                WizardChipsPicker(
                                    Ability.entries.filter { scores.score(it) < 20 },
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

// ── Weapon Mastery ───────────────────────────────────────────────────────────

@Composable
private fun WeaponMasteryStep2024(slots: Int, masteries: Set<String>, onChange: (Set<String>) -> Unit) {
    StepColumn2024 {
        WizardStepSection(
            "Weapon Masteries",
            "Choose $slots weapons to master — ${masteries.size} / $slots chosen.",
        )
        val atCap = masteries.size >= slots
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Weapons2024.all.forEach { weapon ->
                val selected = weapon.id in masteries
                SelectableRow2024(
                    title = "${weapon.name} — ${weapon.mastery.displayName}",
                    subtitle = weapon.mastery.summary,
                    detail = "${categoryLabel(weapon.category)} · ${weapon.damage}",
                    selected = selected,
                    dimmed = !selected && atCap,
                ) { onChange(if (selected) masteries - weapon.id else masteries + weapon.id) }
            }
        }
    }
}

private fun categoryLabel(category: Weapon2024.Category): String =
    category.name.lowercase().replaceFirstChar { it.uppercase() }

// ── Fighting Style ───────────────────────────────────────────────────────────

@Composable
private fun FightingStyleStep2024(selected: String?, onPick: (String?) -> Unit) {
    StepColumn2024 {
        WizardStepSection("Fighting Style", "Choose the style your class's Fighting Style feature grants.")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Feats2024.inCategory(FeatCategory2024.FIGHTING_STYLE).forEach { style ->
                SelectableRow2024(
                    title = style.name,
                    subtitle = style.description,
                    selected = selected == style.id,
                ) { onPick(if (selected == style.id) null else style.id) }
            }
        }
    }
}

// ── Spells ───────────────────────────────────────────────────────────────────

/** The SRD 5.2 library filtered to [classId]'s list at [spellLevel], alphabetical. */
private fun spellPool2024(classId: String?, spellLevel: Int): List<Spell2024> =
    if (classId == null) {
        emptyList()
    } else {
        DnD5e2024Catalog.spellLibrary
            .filter { it.level == spellLevel && it.classes.any { c -> c.equals(classId, ignoreCase = true) } }
            .sortedBy { it.name }
    }

@Composable
private fun SpellsStep2024(
    cantripPool: List<Spell2024>,
    leveledPool: List<Spell2024>,
    cantripCap: Int,
    leveledCap: Int,
    cantrips: Set<String>,
    spells: Set<String>,
    onCantrips: (Set<String>) -> Unit,
    onSpells: (Set<String>) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    StepColumn2024 {
        if (cantripCap > 0) {
            WizardStepSection("Cantrips (${cantrips.size}/$cantripCap)")
            val atCap = cantrips.size >= cantripCap
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cantripPool.forEach { spell ->
                    val selected = spell.id in cantrips
                    SelectableRow2024(
                        title = spell.name,
                        detail = spellMeta2024(spell),
                        selected = selected,
                        dimmed = !selected && atCap,
                    ) { onCantrips(if (selected) cantrips - spell.id else cantrips + spell.id) }
                }
            }
        }
        if (leveledCap > 0) {
            WizardStepSection("1st-Level Spells (${spells.size}/$leveledCap)")
            val atCap = spells.size >= leveledCap
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                leveledPool.forEach { spell ->
                    val selected = spell.id in spells
                    SelectableRow2024(
                        title = spell.name,
                        detail = spellMeta2024(spell),
                        selected = selected,
                        dimmed = !selected && atCap,
                    ) { onSpells(if (selected) spells - spell.id else spells + spell.id) }
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

private fun spellMeta2024(spell: Spell2024): String = listOfNotNull(
    spell.school.ifEmpty { null },
    "Concentration".takeIf { spell.concentration },
    "Ritual".takeIf { spell.ritual },
).joinToString(" · ")

// ── Armor ────────────────────────────────────────────────────────────────────

@Composable
private fun ArmorStep2024(
    klass: CharacterClass2024?,
    armorId: String?,
    hasShield: Boolean,
    previewAC: Int,
    onArmor: (String?) -> Unit,
    onShield: (Boolean) -> Unit,
) {
    klass ?: return
    StepColumn2024 {
        Spacer(Modifier.height(18.dp))
        Banner2024("Armor Class", "$previewAC")
        if (klass.hasShieldProficiency) {
            Spacer(Modifier.height(10.dp))
            SelectableRow2024(
                title = "Shield",
                subtitle = "+${Armors2024.SHIELD_BONUS} AC",
                selected = hasShield,
            ) { onShield(!hasShield) }
        }
        WizardStepSection("Worn Armor", "Only armor your class is trained in can be worn.")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SelectableRow2024(
                title = "None",
                detail = "Unarmored — 10 + DEX, or your Unarmored Defense",
                selected = armorId == null,
            ) { onArmor(null) }
            Armors2024.all.forEach { armor ->
                val proficient = armor.category in klass.armorProficiencies
                val detail = buildString {
                    append(categoryLabel2024(armor.category))
                    append(" · base ${armor.baseAC}")
                    append(
                        when (armor.category) {
                            Armor2024.Category.LIGHT -> " + DEX"
                            Armor2024.Category.MEDIUM -> " + DEX (max 2)"
                            Armor2024.Category.HEAVY -> " · —"
                        },
                    )
                    if (!proficient) append("  (not proficient)")
                }
                SelectableRow2024(
                    title = armor.name,
                    detail = detail,
                    selected = armorId == armor.id,
                    dimmed = !proficient,
                    dimmedAlpha = 0.4f,
                ) { onArmor(armor.id) }
            }
        }
    }
}

private fun categoryLabel2024(category: Armor2024.Category): String =
    category.name.lowercase().replaceFirstChar { it.uppercase() }

// ── Review ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewStep2024(
    name: String,
    level: Int,
    species: Species2024?,
    klass: CharacterClass2024?,
    subclass: String?,
    background: Background2024?,
    scores: AbilityScores,
    increases: Map<Ability, Int>,
    advFeats: List<String>,
    backgroundSkills: List<String>,
    chosenSkills: Set<String>,
    armorId: String?,
    hasShield: Boolean,
    fightingStyle: String?,
    masteries: List<String>,
    cantrips: List<Spell2024>,
    spells: List<Spell2024>,
    onEdit: (Wiz2024) -> Unit,
    isActive: (Wiz2024) -> Boolean,
) {
    val palette = MaterialTheme.natPalette
    val subclassName = subclass?.let { id -> klass?.subclasses?.firstOrNull { it.id == id }?.name ?: id }
    // Pencil-jumps appear only for steps that are actually in the flow.
    fun edit(target: Wiz2024): (() -> Unit)? = if (isActive(target)) ({ onEdit(target) }) else null
    StepColumn2024 {
        Spacer(Modifier.height(18.dp))
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
                species?.let {
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

        ReviewSectionHeader("Identity", edit(Wiz2024.NAME))
        ReviewLine("Name", name.trim())
        ReviewLine("Level", "$level")

        ReviewSectionHeader("Species", edit(Wiz2024.SPECIES))
        ReviewLine("Species", species?.name.orEmpty())

        ReviewSectionHeader("Class", edit(Wiz2024.CLASS))
        ReviewLine("Class", klass?.name.orEmpty())
        subclassName?.let { ReviewLine("Subclass", it) }

        if (subclassName != null) {
            ReviewSectionHeader("Subclass", edit(Wiz2024.SUBCLASS))
            ReviewLine("Subclass", subclassName)
        }

        ReviewSectionHeader("Background", edit(Wiz2024.BACKGROUND))
        ReviewLine("Background", background?.name.orEmpty())
        ReviewLine("Origin feat", background?.originFeat?.let { id -> Feats2024.feat(id)?.name ?: titleCase2024(id) }.orEmpty())

        ReviewSectionHeader("Abilities", edit(Wiz2024.ABILITIES))
        FinalScoresGrid2024(scores, increases)

        if (advFeats.isNotEmpty()) {
            ReviewSectionHeader("Feats", edit(Wiz2024.ADVANCEMENTS))
            ReviewLine("Feats", advFeats.joinToString(" · ") { id -> Feats2024.feat(id)?.name ?: titleCase2024(id) })
        }

        ReviewSectionHeader("Skill Proficiencies", edit(Wiz2024.SKILLS))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            backgroundSkills.forEach { GrantedSkillChip(skill2024(it)) }
            chosenSkills.forEach { id ->
                WizardChip(label = skill2024(id), selected = false, strongFill = true) {}
            }
        }
        if (backgroundSkills.isEmpty() && chosenSkills.isEmpty()) ReviewLine("Skills", "")

        ReviewSectionHeader("Armor", edit(Wiz2024.ARMOR))
        ReviewLine("Worn", armorId?.let { Armors2024.armor(it)?.name ?: it } ?: "None")
        ReviewLine("Shield", if (hasShield) "Shield (+${Armors2024.SHIELD_BONUS} AC)" else "")

        fightingStyle?.let { id ->
            ReviewSectionHeader("Fighting Style", edit(Wiz2024.FIGHTING_STYLE))
            ReviewLine("Style", Feats2024.feat(id)?.name ?: titleCase2024(id))
        }

        if (masteries.isNotEmpty()) {
            ReviewSectionHeader("Weapon Mastery", edit(Wiz2024.WEAPON_MASTERY))
            masteries.forEach { id ->
                Weapons2024.weapon(id)?.let { weapon ->
                    ReviewLine(weapon.name, weapon.mastery.displayName)
                }
            }
        }

        if (cantrips.isNotEmpty()) {
            ReviewSectionHeader("Cantrips", edit(Wiz2024.SPELLS))
            ReviewLine("Cantrips", cantrips.joinToString(" · ") { it.name })
        }
        if (spells.isNotEmpty()) {
            ReviewSectionHeader("1st-Level Spells", edit(Wiz2024.SPELLS))
            ReviewLine("Spells", spells.joinToString(" · ") { it.name })
        }
    }
}

// ── Shared helpers ───────────────────────────────────────────────────────────

private fun skill2024(id: String): String = DnD5eCatalog.skill(id)?.name ?: id

private fun titleCase2024(slug: String): String =
    slug.split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

/** Base scores plus the background increases, each capped at 20. */
private fun AbilityScores.applying(bonus: Map<Ability, Int>): AbilityScores =
    Ability.entries.fold(this) { acc, ability -> acc.with(ability, minOf(20, acc.score(ability) + (bonus[ability] ?: 0))) }
