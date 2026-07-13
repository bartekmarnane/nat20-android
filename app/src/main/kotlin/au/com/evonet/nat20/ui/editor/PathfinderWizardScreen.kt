package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.pf2e.Bulk
import au.com.evonet.nat20.pf2e.FeatSlotKey
import au.com.evonet.nat20.pf2e.FeatSlots
import au.com.evonet.nat20.pf2e.PathfinderBuilder
import au.com.evonet.nat20.pf2e.PathfinderCatalog
import au.com.evonet.nat20.pf2e.PathfinderPayload
import au.com.evonet.nat20.pf2e.PathfinderRuleset
import au.com.evonet.nat20.pf2e.PfAncestry
import au.com.evonet.nat20.pf2e.PfArmors
import au.com.evonet.nat20.pf2e.PfBackground
import au.com.evonet.nat20.pf2e.PfClass
import au.com.evonet.nat20.pf2e.PfFeat
import au.com.evonet.nat20.pf2e.PfFeatType
import au.com.evonet.nat20.pf2e.PfFeats
import au.com.evonet.nat20.pf2e.PfShields
import au.com.evonet.nat20.pf2e.PfSpell
import au.com.evonet.nat20.pf2e.PfSpells
import au.com.evonet.nat20.pf2e.PfWeapons
import au.com.evonet.nat20.pf2e.Wealth
import au.com.evonet.nat20.pf2e.armorClass
import au.com.evonet.nat20.pf2e.classDcValue
import au.com.evonet.nat20.pf2e.core.AdvancementSchedule
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.SpellcastingProgression
import au.com.evonet.nat20.pf2e.encumberedThreshold
import au.com.evonet.nat20.pf2e.perceptionBonus
import au.com.evonet.nat20.pf2e.strike
import au.com.evonet.nat20.ui.slugToTitle
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette
import java.time.Instant

/**
 * The Pathfinder 2e (Remaster) creation wizard, restyled to iOS parity on the
 * shared wizard atoms: Name → Ancestry (+heritage) → Background → Class (+key
 * ability + starting level) → Subclass (classes with one) → Abilities (the
 * boost/flaw build) → Skills → Spells (casters) → Feats → Advancement (level
 * 2+) → Equipment → Review. Above-level-1 characters replay the Level Up maths
 * through [PathfinderBuilder]; the Review step pencil-jumps back into any step.
 */
private enum class PfWiz(val title: String) {
    NAME("Name"), ANCESTRY("Ancestry"), BACKGROUND("Background"), CLASS("Class"),
    SUBCLASS("Subclass"), ABILITIES("Abilities"), SKILLS("Skills"), SPELLS("Spells"),
    FEATS("Feats"), ADVANCEMENT("Advancement"), EQUIPMENT("Equipment"), REVIEW("Review"),
}

/** The feat-slot types earned at [level] (2+) on the class's clocks — the Advancement step's picks. */
private fun featSlotsAt(classId: String, level: Int): List<PfFeatType> = buildList {
    if (level in AdvancementSchedule.ANCESTRY_FEAT_LEVELS && level > 1) add(PfFeatType.ANCESTRY)
    if (level in FeatSlots.classFeatLevels(classId) && level > 1) add(PfFeatType.CLASS)
    if (level in AdvancementSchedule.skillFeatLevels(classId == "rogue") && level > 1) add(PfFeatType.SKILL)
    if (level in AdvancementSchedule.GENERAL_FEAT_LEVELS) add(PfFeatType.GENERAL)
}

@Composable
fun PathfinderWizardScreen(
    onSave: (Character) -> Unit,
    onCancel: () -> Unit,
    stepOffset: Int = 0,
    onExitFirstStep: (() -> Unit)? = null,
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var ancestryId by rememberSaveable { mutableStateOf<String?>(null) }
    var heritage by rememberSaveable { mutableStateOf<String?>(null) }
    var backgroundId by rememberSaveable { mutableStateOf<String?>(null) }
    var classId by rememberSaveable { mutableStateOf<String?>(null) }
    var level by rememberSaveable { mutableIntStateOf(1) }
    var keyAbility by remember { mutableStateOf<PfAbility?>(null) }
    var subclass by rememberSaveable { mutableStateOf<String?>(null) }
    var ancestryFree by remember { mutableStateOf<List<PfAbility>>(emptyList()) }
    var backgroundBoost by remember { mutableStateOf<PfAbility?>(null) }
    var backgroundFree by remember { mutableStateOf<PfAbility?>(null) }
    var freeBoosts by remember { mutableStateOf<List<PfAbility>>(emptyList()) }
    var chosenSkills by remember { mutableStateOf<Set<PfSkill>>(emptySet()) }
    var chosenCantrips by remember { mutableStateOf<Set<String>>(emptySet()) }
    var chosenRank1 by remember { mutableStateOf<Set<String>>(emptySet()) }
    var ancestryFeat by rememberSaveable { mutableStateOf<String?>(null) }
    var classFeat by rememberSaveable { mutableStateOf<String?>(null) }
    var advBoosts by remember { mutableStateOf<Map<Int, List<PfAbility>>>(emptyMap()) }
    var skillIncs by remember { mutableStateOf<Map<Int, PfSkill>>(emptyMap()) }
    var advFeats by remember { mutableStateOf<Map<FeatSlotKey, String>>(emptyMap()) }
    var armorId by rememberSaveable { mutableStateOf<String?>(null) }
    var shieldId by rememberSaveable { mutableStateOf<String?>(null) }
    var weaponIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Pencil-jump from Review: the footer collapses to a single "Done" back to Review.
    var editingFromReview by rememberSaveable { mutableStateOf(false) }

    val ancestry = ancestryId?.let(PathfinderCatalog::ancestry)
    val background = backgroundId?.let(PathfinderCatalog::background)
    val cls = classId?.let(PathfinderCatalog::pfClass)
    val tradition = cls?.tradition
    val pickedSubclass = cls?.subclasses?.firstOrNull { it.id == subclass }

    // Spell pools + quotas (presentation capped at what the catalogue offers).
    val cantripPool = tradition?.let { t -> PfSpells.forTradition(t).filter { it.rank == 0 } }.orEmpty()
    val rank1Pool = tradition?.let { t -> PfSpells.forTradition(t).filter { it.rank == 1 } }.orEmpty()
    val cantripCap = minOf(SpellcastingProgression.CANTRIPS_PER_DAY, cantripPool.size)
    val rank1Cap = minOf(SpellcastingProgression.fullCasterSlots(level)[1] ?: 0, rank1Pool.size)

    // Level-1 feat pools (the FEATS step gates only on groups that offer options).
    val ancestryFeatPool = ancestryId?.let { PfFeats.available(PfFeatType.ANCESTRY, it, classId.orEmpty(), 1) }.orEmpty()
    val classFeatPool = classId?.let { PfFeats.available(PfFeatType.CLASS, ancestryId.orEmpty(), it, 1) }.orEmpty()

    fun choices() = PathfinderBuilder.Choices(
        name = name, ancestryId = ancestryId.orEmpty(), heritage = heritage, backgroundId = backgroundId.orEmpty(),
        classId = classId.orEmpty(), keyAbility = keyAbility ?: PfAbility.STRENGTH,
        ancestryFreeBoosts = ancestryFree, backgroundBoost = backgroundBoost, backgroundFreeBoost = backgroundFree,
        freeBoosts = freeBoosts, chosenSkills = chosenSkills.toList(),
        targetLevel = level, subclass = subclass,
        cantrips = cantripPool.filter { it.id in chosenCantrips }.map { it.id }.take(cantripCap),
        rank1Spells = rank1Pool.filter { it.id in chosenRank1 }.map { it.id }.take(rank1Cap),
        ancestryFeat = ancestryFeat, classFeat = classFeat,
        advancementBoosts = advBoosts, skillIncreases = skillIncs, advancementFeats = advFeats,
        armor = armorId, shield = shieldId,
        weapons = PfWeapons.all.filter { it.id in weaponIds }.map { it.id },
    )

    val skillSlots = if (cls != null && keyAbility != null) PathfinderBuilder.skillSlots(choices()) else 0
    val grantedSkills = listOfNotNull(background?.trainedSkill, pickedSubclass?.grantedSkill).distinct()

    /** The trained-skill ranks entering [atLevel]: the creation set + increases taken below it. */
    fun skillRanksBefore(atLevel: Int): Map<PfSkill, Proficiency> {
        val ranks = mutableMapOf<PfSkill, Proficiency>()
        grantedSkills.forEach { ranks[it] = Proficiency.TRAINED }
        chosenSkills.forEach { ranks[it] = Proficiency.TRAINED }
        for (l in 2 until atLevel) {
            val skill = skillIncs[l]?.takeIf { AdvancementSchedule.grantsSkillIncrease(l) } ?: continue
            val raised = (ranks[skill] ?: Proficiency.UNTRAINED).next ?: continue
            if (raised.rank <= AdvancementSchedule.maxSkillRank(l).rank) ranks[skill] = raised
        }
        return ranks
    }

    /** Skills a level-[atLevel] increase may raise: trained or better, next rank under the level cap. */
    fun eligibleIncreases(atLevel: Int): List<PfSkill> {
        val ranks = skillRanksBefore(atLevel)
        return PfSkill.entries.filter { s ->
            val current = ranks[s] ?: Proficiency.UNTRAINED
            current != Proficiency.UNTRAINED &&
                current.next?.let { it.rank <= AdvancementSchedule.maxSkillRank(atLevel).rank } == true
        }
    }

    fun advancementComplete(atLevel: Int): Boolean {
        if (AdvancementSchedule.grantsAbilityBoosts(atLevel) &&
            (advBoosts[atLevel]?.size ?: 0) != AdvancementSchedule.BOOSTS_PER_ABILITY_BOOST_LEVEL
        ) return false
        if (AdvancementSchedule.grantsSkillIncrease(atLevel) && eligibleIncreases(atLevel).isNotEmpty() &&
            skillIncs[atLevel] == null
        ) return false
        return featSlotsAt(classId.orEmpty(), atLevel).all { type ->
            PfFeats.available(type, ancestryId.orEmpty(), classId.orEmpty(), atLevel).isEmpty() ||
                advFeats[FeatSlotKey(atLevel, type)] != null
        }
    }

    val steps = PfWiz.entries.filter {
        when (it) {
            PfWiz.SUBCLASS -> cls?.hasSubclass == true
            PfWiz.SPELLS -> tradition != null
            PfWiz.ADVANCEMENT -> level > 1
            else -> true
        }
    }
    val step = steps[stepIndex.coerceIn(0, steps.lastIndex)]

    fun canAdvance(): Boolean = when (step) {
        PfWiz.NAME -> name.isNotBlank()
        PfWiz.ANCESTRY -> ancestry != null && (ancestry.heritages.isEmpty() || heritage != null)
        PfWiz.BACKGROUND -> background != null
        PfWiz.CLASS -> cls != null && keyAbility != null
        PfWiz.SUBCLASS -> pickedSubclass != null
        PfWiz.ABILITIES -> ancestry != null && ancestryFree.size == ancestry.freeBoosts &&
            backgroundBoost != null && backgroundFree != null && freeBoosts.size == 4
        PfWiz.SKILLS -> chosenSkills.size == skillSlots
        PfWiz.SPELLS -> true // under-picking is allowed; the Spells tab can fill the rest
        PfWiz.FEATS -> (ancestryFeatPool.isEmpty() || ancestryFeat != null) && (classFeatPool.isEmpty() || classFeat != null)
        PfWiz.ADVANCEMENT -> (2..level).all { advancementComplete(it) }
        PfWiz.EQUIPMENT -> true // Load & Cost is advisory, never a gate
        PfWiz.REVIEW -> true
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
            if (editingFromReview && step != PfWiz.REVIEW) {
                Spacer(Modifier.weight(1f))
                WizardPrimaryButton("Done", enabled = canAdvance()) { jumpToReview() }
            } else {
                WizardSecondaryButton("Cancel", onCancel)
                Spacer(Modifier.weight(1f))
                WizardPrimaryButton(
                    label = if (step == PfWiz.REVIEW) "Create" else "Continue",
                    enabled = canAdvance(),
                ) {
                    if (step == PfWiz.REVIEW) {
                        onSave(Character.new(name.trim(), PathfinderRuleset(), PathfinderBuilder.build(choices()), Instant.now()))
                    } else {
                        stepIndex++
                    }
                }
            }
        },
    ) {
        Column(Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                PfWiz.NAME -> PfStepColumn { IdentityStep(name, { name = it }) }
                PfWiz.ANCESTRY -> PfAncestryStep(
                    ancestry = ancestry, heritage = heritage,
                    onPick = { picked ->
                        if (picked.id != ancestryId) {
                            ancestryId = picked.id
                            heritage = null
                            ancestryFree = emptyList()
                            ancestryFeat = null
                            advFeats = advFeats.filterKeys { it.type != PfFeatType.ANCESTRY }
                        }
                    },
                    onHeritage = { heritage = it },
                )
                PfWiz.BACKGROUND -> PfBackgroundStep(background) { picked ->
                    if (picked.id != backgroundId) {
                        backgroundId = picked.id
                        backgroundBoost = null
                        backgroundFree = null
                    }
                }
                PfWiz.CLASS -> PfClassStep(
                    cls = cls, keyAbility = keyAbility, level = level,
                    onPick = { picked ->
                        if (picked.id != classId) {
                            // iOS setClass rule: switching class clears every class-derived pick.
                            classId = picked.id
                            keyAbility = picked.keyAbilityOptions.singleOrNull()
                            subclass = null
                            chosenSkills = emptySet()
                            chosenCantrips = emptySet()
                            chosenRank1 = emptySet()
                            classFeat = null
                            advFeats = advFeats.filterKeys { it.type == PfFeatType.ANCESTRY }
                            armorId = null
                            shieldId = null
                            weaponIds = emptySet()
                        }
                    },
                    onKeyAbility = { keyAbility = it },
                    onLevel = { level = it },
                )
                PfWiz.SUBCLASS -> PfSubclassStep(cls, subclass) { subclass = it }
                PfWiz.ABILITIES -> PfAbilitiesStep(
                    ancestry = ancestry, background = background, keyAbility = keyAbility,
                    scores = PathfinderBuilder.scores(choices()),
                    ancestryFree = ancestryFree, backgroundBoost = backgroundBoost,
                    backgroundFree = backgroundFree, freeBoosts = freeBoosts,
                    onAncestryFree = { ancestryFree = it },
                    onBackgroundBoost = { backgroundBoost = it },
                    onBackgroundFree = { backgroundFree = it },
                    onFreeBoosts = { freeBoosts = it },
                )
                PfWiz.SKILLS -> PfSkillsStep(
                    cls = cls, background = background, grantedSkills = grantedSkills,
                    quota = skillSlots, chosen = chosenSkills,
                ) { chosenSkills = it }
                PfWiz.SPELLS -> PfSpellsStep(
                    cantripPool = cantripPool, rank1Pool = rank1Pool,
                    cantripCap = cantripCap, rank1Cap = rank1Cap,
                    cantrips = chosenCantrips, rank1 = chosenRank1,
                    onCantrips = { chosenCantrips = it },
                    onRank1 = { chosenRank1 = it },
                )
                PfWiz.FEATS -> PfFeatsStep(
                    ancestryPool = ancestryFeatPool, classPool = classFeatPool,
                    ancestryFeat = ancestryFeat, classFeat = classFeat,
                    onAncestryFeat = { ancestryFeat = it },
                    onClassFeat = { classFeat = it },
                )
                PfWiz.ADVANCEMENT -> PfAdvancementStep(
                    cls = cls, ancestryId = ancestryId.orEmpty(), level = level,
                    advBoosts = advBoosts, skillIncs = skillIncs, advFeats = advFeats,
                    eligibleIncreases = ::eligibleIncreases,
                    onBoosts = { lvl, list -> advBoosts = advBoosts + (lvl to list) },
                    onSkillInc = { lvl, skill -> skillIncs = skillIncs + (lvl to skill) },
                    onFeat = { key, id -> advFeats = advFeats + (key to id) },
                )
                PfWiz.EQUIPMENT -> PfEquipmentStep(
                    cls = cls, level = level,
                    preview = PathfinderBuilder.build(choices()),
                    armorId = armorId, shieldId = shieldId, weaponIds = weaponIds,
                    onArmor = { armorId = it },
                    onShield = { shieldId = it },
                    onWeapons = { weaponIds = it },
                )
                PfWiz.REVIEW -> PfReviewStep(
                    name = name, level = level, ancestry = ancestry, heritage = heritage,
                    background = background, cls = cls, subclassName = pickedSubclass?.name,
                    keyAbility = keyAbility,
                    payload = PathfinderBuilder.build(choices()),
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
private fun PfStepColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, bottom = 16.dp),
        content = content,
    )
}

/** Accent banner row: Cinzel label + big Cormorant value (Armor Class on the Equipment step). */
@Composable
private fun PfBanner(label: String, value: String) {
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
        Text(value, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = palette.accent)
    }
}

/** Italic IM Fell helper line (quota hints, advisory notes, empty-catalogue placeholders). */
@Composable
private fun PfHintLine(text: String, danger: Boolean = false) {
    val palette = MaterialTheme.natPalette
    Text(
        text,
        fontFamily = ImFell,
        fontStyle = FontStyle.Italic,
        fontSize = 12.sp,
        color = if (danger) palette.danger else palette.inkMute,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** Italic detail-card title (ancestry/background/class/subclass names). */
@Composable
private fun PfCardTitle(text: String) {
    Text(
        text,
        fontFamily = Cormorant,
        fontWeight = FontWeight.SemiBold,
        fontStyle = FontStyle.Italic,
        fontSize = 22.sp,
        color = MaterialTheme.natPalette.accent,
    )
}

/** Multi-select ability-boost chips: [limit] distinct picks, [blocked] abilities disabled. */
@Composable
private fun PfBoostChips(
    selected: List<PfAbility>,
    limit: Int,
    blocked: Set<PfAbility> = emptySet(),
    onChange: (List<PfAbility>) -> Unit,
) {
    WizardMultiSelectChips(
        items = PfAbility.entries,
        isSelected = { it in selected },
        label = { it.abbreviation },
        quotaFull = selected.size >= limit,
        enabled = { it !in blocked },
    ) { ability -> onChange(if (ability in selected) selected - ability else selected + ability) }
}

/** The six resolved ability tiles (abbr / score / modifier) — Abilities preview + Review. */
@Composable
private fun PfScoresGrid(scores: PfAbilityScores) {
    val palette = MaterialTheme.natPalette
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PfAbility.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { ability ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(palette.tile)
                            .border(1.dp, palette.accent.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(ability.abbreviation, fontFamily = Cinzel, fontSize = 11.sp, color = palette.inkMute)
                        Text(
                            "${scores.score(ability)}",
                            fontFamily = Cormorant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                            color = palette.ink,
                        )
                        val mod = PfAbilityScores.modifier(scores.score(ability))
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

/** "15 gp" / "0.2 gp" — PF2e prices are decimal; whole numbers drop the fraction. */
private fun gpLabel(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()} gp" else "${"%.1f".format(value)} gp"

// ── Ancestry ─────────────────────────────────────────────────────────────────

@Composable
private fun PfAncestryStep(
    ancestry: PfAncestry?,
    heritage: String?,
    onPick: (PfAncestry) -> Unit,
    onHeritage: (String) -> Unit,
) {
    PfStepColumn {
        WizardStepSection("Choose an Ancestry", "Ancestry sets your starting HP, size, speed, and ability boosts.")
        WizardChipsPicker(PathfinderCatalog.ancestries, { it.id == ancestry?.id }, { it.name }, large = true, onPick = onPick)
        if (ancestry != null) {
            Spacer(Modifier.height(12.dp))
            WizardDetailCard {
                PfCardTitle(ancestry.name)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WizardStatCell("HP", "${ancestry.hp}", modifier = Modifier.weight(1f))
                    WizardStatCell("Size", ancestry.size, modifier = Modifier.weight(1f))
                    WizardStatCell("Speed", "${ancestry.speed} ft", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                val boosts = buildList {
                    ancestry.boosts.forEach { add("+2 ${it.abbreviation}") }
                    if (ancestry.freeBoosts > 0) add("${ancestry.freeBoosts} free boost${if (ancestry.freeBoosts > 1) "s" else ""}")
                    ancestry.flaw?.let { add("−2 ${it.abbreviation}") }
                }
                WizardStatCell("Boosts", boosts.joinToString(" · "), modifier = Modifier.fillMaxWidth())
            }
            if (ancestry.heritages.isNotEmpty()) {
                WizardStepSection("Heritage", "Every ${ancestry.name.lowercase()} inherits one lineage.")
                WizardChipsPicker(ancestry.heritages, { it == heritage }, { it.slugToTitle() }, onPick = onHeritage)
            }
        }
    }
}

// ── Background ───────────────────────────────────────────────────────────────

@Composable
private fun PfBackgroundStep(background: PfBackground?, onPick: (PfBackground) -> Unit) {
    PfStepColumn {
        WizardStepSection("Choose a Background", "Backgrounds train a skill and a Lore, and grant two ability boosts.")
        WizardChipsPicker(PathfinderCatalog.backgrounds, { it.id == background?.id }, { it.name }, large = true, onPick = onPick)
        if (background != null) {
            Spacer(Modifier.height(12.dp))
            WizardDetailCard {
                PfCardTitle(background.name)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WizardStatCell("Trained skill", background.trainedSkill.displayName, modifier = Modifier.weight(1f))
                    WizardStatCell("Lore", background.lore, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                WizardStatCell(
                    "Boosts",
                    background.boostOptions.joinToString(" or ") { it.abbreviation } + " · one free",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PfHintLine("You'll allocate the boosts on the Abilities step.")
        }
    }
}

// ── Class (+ key ability + starting level) ───────────────────────────────────

@Composable
private fun PfClassStep(
    cls: PfClass?,
    keyAbility: PfAbility?,
    level: Int,
    onPick: (PfClass) -> Unit,
    onKeyAbility: (PfAbility) -> Unit,
    onLevel: (Int) -> Unit,
) {
    PfStepColumn {
        WizardStepSection("Pick Class")
        WizardChipsPicker(PathfinderCatalog.classes, { it.id == cls?.id }, { it.name }, large = true, onPick = onPick)
        if (cls != null) {
            Spacer(Modifier.height(12.dp))
            WizardDetailCard {
                PfCardTitle(cls.name)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WizardStatCell("HP", "${cls.hpPerLevel}/level", modifier = Modifier.weight(1f))
                    WizardStatCell("Key", cls.keyAbilityOptions.joinToString(" or ") { it.abbreviation }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                WizardStatCell(
                    "Magic",
                    cls.tradition?.let { "Casts ${it.displayName.lowercase()} spells" } ?: "Not a spellcaster",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (cls.hasSubclass) {
                    Spacer(Modifier.height(6.dp))
                    WizardStatCell("Subclass", "Choose a ${cls.subclassLabel} next", modifier = Modifier.fillMaxWidth())
                }
            }
            if (cls.keyAbilityOptions.size > 1) {
                WizardStepSection("Key Ability", "Drives your class DC${if (cls.tradition != null) " and spellcasting" else ""}.")
                WizardChipsPicker(cls.keyAbilityOptions, { it == keyAbility }, { it.displayName }, onPick = onKeyAbility)
            }
            WizardStepSection("Starting Level")
            WizardLevelStepper("Character level", level) { onLevel(it) }
            if (level > 1) {
                PfHintLine("Levels 2–$level bring advancement choices — you'll make them in a later step.")
            }
        }
    }
}

// ── Subclass ─────────────────────────────────────────────────────────────────

@Composable
private fun PfSubclassStep(cls: PfClass?, subclass: String?, onPick: (String) -> Unit) {
    cls ?: return
    PfStepColumn {
        WizardStepSection("Choose a ${cls.subclassLabel}", "The ${cls.name}'s defining choice, made at level 1.")
        WizardChipsPicker(cls.subclasses, { it.id == subclass }, { it.name }, large = true) { onPick(it.id) }
        cls.subclasses.firstOrNull { it.id == subclass }?.let { picked ->
            Spacer(Modifier.height(12.dp))
            WizardDetailCard {
                PfCardTitle(picked.name)
                Text(
                    picked.summary,
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = MaterialTheme.natPalette.inkSoft,
                    modifier = Modifier.padding(top = 4.dp),
                )
                picked.grantedSkill?.let {
                    Spacer(Modifier.height(8.dp))
                    WizardStatCell("Trained skill", it.displayName, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ── Abilities (the PF2e boost/flaw build) ────────────────────────────────────

@Composable
private fun PfAbilitiesStep(
    ancestry: PfAncestry?,
    background: PfBackground?,
    keyAbility: PfAbility?,
    scores: PfAbilityScores,
    ancestryFree: List<PfAbility>,
    backgroundBoost: PfAbility?,
    backgroundFree: PfAbility?,
    freeBoosts: List<PfAbility>,
    onAncestryFree: (List<PfAbility>) -> Unit,
    onBackgroundBoost: (PfAbility) -> Unit,
    onBackgroundFree: (PfAbility) -> Unit,
    onFreeBoosts: (List<PfAbility>) -> Unit,
) {
    PfStepColumn {
        WizardStepSection(
            "Ability Boosts",
            "A boost is +2 — or +1 once a score reaches 18. No ability twice in one group.",
        )
        keyAbility?.let {
            WizardFieldLabel("Class key ability")
            Row { GrantedSkillChip("+2 ${it.displayName}") }
            Spacer(Modifier.height(10.dp))
        }
        if (ancestry != null && ancestry.freeBoosts > 0) {
            WizardFieldLabel(
                "${ancestry.name} free boosts",
                hint = "${ancestryFree.size} / ${ancestry.freeBoosts}",
            )
            // Fixed ancestry boosts share this group, so they can't be picked again.
            PfBoostChips(ancestryFree, ancestry.freeBoosts, blocked = ancestry.boosts.toSet(), onChange = onAncestryFree)
            Spacer(Modifier.height(10.dp))
        }
        if (background != null) {
            WizardFieldLabel("${background.name} boost", hint = "choose one")
            WizardChipsPicker(background.boostOptions, { it == backgroundBoost }, { it.abbreviation }, onPick = onBackgroundBoost)
            Spacer(Modifier.height(10.dp))
            WizardFieldLabel("${background.name} free boost")
            WizardChipsPicker(
                PfAbility.entries.filter { it != backgroundBoost },
                { it == backgroundFree },
                { it.abbreviation },
                onPick = onBackgroundFree,
            )
            Spacer(Modifier.height(10.dp))
        }
        WizardFieldLabel("Four free boosts", hint = "${freeBoosts.size} / 4")
        PfBoostChips(freeBoosts, 4, onChange = onFreeBoosts)
        WizardStepSection("Preview")
        PfScoresGrid(scores)
    }
}

// ── Skills ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PfSkillsStep(
    cls: PfClass?,
    background: PfBackground?,
    grantedSkills: List<PfSkill>,
    quota: Int,
    chosen: Set<PfSkill>,
    onChange: (Set<PfSkill>) -> Unit,
) {
    PfStepColumn {
        if (grantedSkills.isNotEmpty()) {
            WizardStepSection("Already trained", background?.name?.let { "From your background and subclass." })
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                grantedSkills.forEach { GrantedSkillChip(it.displayName) }
            }
        }
        WizardStepSection(
            "Class Skills",
            "${cls?.name ?: "Class"} · pick $quota — ${chosen.size} / $quota chosen",
        )
        val options = (cls?.classSkills ?: PfSkill.entries).filterNot { it in grantedSkills }
        WizardMultiSelectChips(
            items = options,
            isSelected = { it in chosen },
            label = { "${it.displayName} (${it.ability.abbreviation})" },
            quotaFull = chosen.size >= quota,
        ) { skill -> onChange(if (skill in chosen) chosen - skill else chosen + skill) }
    }
}

// ── Spells (casters) ─────────────────────────────────────────────────────────

@Composable
private fun PfSpellsStep(
    cantripPool: List<PfSpell>,
    rank1Pool: List<PfSpell>,
    cantripCap: Int,
    rank1Cap: Int,
    cantrips: Set<String>,
    rank1: Set<String>,
    onCantrips: (Set<String>) -> Unit,
    onRank1: (Set<String>) -> Unit,
) {
    PfStepColumn {
        PfSpellGroup("Cantrips", cantripPool, cantripCap, cantrips, onCantrips)
        PfSpellGroup("1st-Rank Spells", rank1Pool, rank1Cap, rank1, onRank1)
        Spacer(Modifier.height(10.dp))
        PfHintLine("You can learn or change spells anytime on the Spells tab.")
    }
}

@Composable
private fun PfSpellGroup(
    title: String,
    pool: List<PfSpell>,
    cap: Int,
    chosen: Set<String>,
    onChange: (Set<String>) -> Unit,
) {
    if (pool.isEmpty() || cap <= 0) return
    WizardStepSection("$title (${chosen.size}/$cap)")
    WizardMultiSelectChips(
        items = pool,
        isSelected = { it.id in chosen },
        label = { it.name },
        quotaFull = chosen.size >= cap,
    ) { spell -> onChange(if (spell.id in chosen) chosen - spell.id else chosen + spell.id) }
    pool.filter { it.id in chosen }.forEach { spell ->
        Spacer(Modifier.height(6.dp))
        WizardQuoteBlock("${spell.name} · ${spell.actions}A", spell.summary)
    }
}

// ── Feats (level 1: ancestry + class) ────────────────────────────────────────

@Composable
private fun PfFeatsStep(
    ancestryPool: List<PfFeat>,
    classPool: List<PfFeat>,
    ancestryFeat: String?,
    classFeat: String?,
    onAncestryFeat: (String) -> Unit,
    onClassFeat: (String) -> Unit,
) {
    PfStepColumn {
        PfFeatGroup("Ancestry Feat", ancestryPool, ancestryFeat, onAncestryFeat)
        PfFeatGroup("Class Feat", classPool, classFeat, onClassFeat)
    }
}

@Composable
private fun PfFeatGroup(title: String, pool: List<PfFeat>, chosen: String?, onPick: (String) -> Unit) {
    WizardStepSection(title)
    if (pool.isEmpty()) {
        PfHintLine("No ${title.lowercase()} options catalogued yet — you can take one from the Feats tab later.")
        return
    }
    WizardChipsPicker(pool, { it.id == chosen }, { it.name }, onPick = { onPick(it.id) })
    pool.firstOrNull { it.id == chosen }?.let {
        Spacer(Modifier.height(6.dp))
        WizardQuoteBlock(it.name, it.summary)
    }
}

// ── Advancement (levels 2..target) ───────────────────────────────────────────

@Composable
private fun PfAdvancementStep(
    cls: PfClass?,
    ancestryId: String,
    level: Int,
    advBoosts: Map<Int, List<PfAbility>>,
    skillIncs: Map<Int, PfSkill>,
    advFeats: Map<FeatSlotKey, String>,
    eligibleIncreases: (Int) -> List<PfSkill>,
    onBoosts: (Int, List<PfAbility>) -> Unit,
    onSkillInc: (Int, PfSkill) -> Unit,
    onFeat: (FeatSlotKey, String) -> Unit,
) {
    cls ?: return
    PfStepColumn {
        WizardStepSection(
            "Advancement",
            "Your ${cls.name} is level $level — make the choices earned along the way.",
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (lvl in 2..level) {
                val boosts = advBoosts[lvl].orEmpty()
                val featSlots = featSlotsAt(cls.id, lvl)
                val grantsBoosts = AdvancementSchedule.grantsAbilityBoosts(lvl)
                val grantsSkill = AdvancementSchedule.grantsSkillIncrease(lvl)
                if (!grantsBoosts && !grantsSkill && featSlots.isEmpty()) continue
                WizardSubSectionCard("LEVEL $lvl") {
                    if (grantsBoosts) {
                        Spacer(Modifier.height(8.dp))
                        WizardFieldLabel("Four ability boosts", hint = "${boosts.size} / 4")
                        PfBoostChips(boosts, AdvancementSchedule.BOOSTS_PER_ABILITY_BOOST_LEVEL) { onBoosts(lvl, it) }
                    }
                    if (grantsSkill) {
                        Spacer(Modifier.height(8.dp))
                        val eligible = eligibleIncreases(lvl)
                        WizardFieldLabel("Skill increase", hint = "to ${AdvancementSchedule.maxSkillRank(lvl).displayName} at most")
                        if (eligible.isEmpty()) {
                            PfHintLine("No skill can be raised at this level.")
                        } else {
                            WizardChipsPicker(eligible, { it == skillIncs[lvl] }, { it.displayName }) { onSkillInc(lvl, it) }
                        }
                    }
                    featSlots.forEach { type ->
                        Spacer(Modifier.height(8.dp))
                        val key = FeatSlotKey(lvl, type)
                        val pool = PfFeats.available(type, ancestryId, cls.id, lvl)
                        WizardFieldLabel("${type.displayName} feat")
                        if (pool.isEmpty()) {
                            PfHintLine("No ${type.displayName.lowercase()} feats catalogued yet.")
                        } else {
                            WizardChipsPicker(pool, { it.id == advFeats[key] }, { it.name }) { onFeat(key, it.id) }
                            pool.firstOrNull { it.id == advFeats[key] }?.let {
                                Spacer(Modifier.height(4.dp))
                                WizardQuoteBlock(it.name, it.summary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Equipment ────────────────────────────────────────────────────────────────

@Composable
private fun PfEquipmentStep(
    cls: PfClass?,
    level: Int,
    preview: PathfinderPayload,
    armorId: String?,
    shieldId: String?,
    weaponIds: Set<String>,
    onArmor: (String?) -> Unit,
    onShield: (String?) -> Unit,
    onWeapons: (Set<String>) -> Unit,
) {
    cls ?: return
    val palette = MaterialTheme.natPalette
    PfStepColumn {
        Spacer(Modifier.height(18.dp))
        PfBanner("Armor Class", "${preview.armorClass}")

        WizardStepSection("Armor", "Only armor your ${cls.name} is trained in is offered.")
        val wearable = PfArmors.all.filter { (cls.armorProf[it.category] ?: Proficiency.UNTRAINED) != Proficiency.UNTRAINED }
        PfIdChips(
            options = listOf(null to "Unarmored") + wearable.map { it.id as String? to it.name },
            selected = armorId,
            onPick = onArmor,
        )
        armorId?.let(PfArmors::by)?.let { armor ->
            Spacer(Modifier.height(8.dp))
            WizardStatCell(
                armor.name,
                "${armor.category.displayName} · +${armor.acBonus} item · Dex cap +${armor.dexCap} · ${gpLabel(armor.priceGp)} · ${Bulk.label(armor.bulk)} Bulk",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        WizardStepSection("Shield")
        PfIdChips(
            options = listOf(null to "None") + PfShields.all.map { it.id as String? to it.name },
            selected = shieldId,
            onPick = onShield,
        )
        shieldId?.let(PfShields::by)?.let { shield ->
            Spacer(Modifier.height(8.dp))
            WizardStatCell(
                shield.name,
                "+${shield.raisedAcBonus} AC while raised · Hardness ${shield.hardness} · ${gpLabel(shield.priceGp)}",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        WizardStepSection("Weapons", "Weapons your class is trained in; each becomes a Strike.")
        val usable = PfWeapons.all.filter { (cls.weaponProf[it.category] ?: Proficiency.UNTRAINED) != Proficiency.UNTRAINED }
        WizardMultiSelectChips(
            items = usable,
            isSelected = { it.id in weaponIds },
            label = { it.name },
            quotaFull = false,
        ) { weapon -> onWeapons(if (weapon.id in weaponIds) weaponIds - weapon.id else weaponIds + weapon.id) }
        usable.filter { it.id in weaponIds }.forEach { weapon ->
            val strike = preview.strike(weapon)
            val mod = strike.attackMods.first()
            Spacer(Modifier.height(6.dp))
            WizardQuoteBlock(
                weapon.name,
                "Strike ${if (mod >= 0) "+$mod" else "$mod"} · ${strike.damage} ${strike.damageType} · ${weapon.traits.joinToString(", ").ifEmpty { weapon.category.displayName }}",
            )
        }

        // Load & Cost — advisory only, never a gate.
        WizardStepSection("Load & Cost")
        val kitBulk = Wealth.kitBulk(armorId, shieldId, weaponIds)
        val encumbered = Bulk.effective(kitBulk) > preview.encumberedThreshold
        val kitCost = Wealth.kitCostGp(armorId, shieldId, weaponIds)
        val wealth = Wealth.startingWealthGP(level).toDouble()
        val remaining = wealth - kitCost
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardStatCell(
                "Load",
                Bulk.label(kitBulk) + " Bulk" + if (encumbered) " · Encumbered" else "",
                danger = encumbered,
                modifier = Modifier.weight(1f),
            )
            WizardStatCell("Kit cost", gpLabel(kitCost), modifier = Modifier.weight(1f))
        }
        Text(
            "Kit ${gpLabel(kitCost)} · starting wealth ${gpLabel(wealth)} · ${gpLabel(remaining)} remaining",
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp,
            color = if (remaining < 0) palette.danger else palette.inkMute,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Single-select chips over (id, label) options where a null id is the "none" pick. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PfIdChips(options: List<Pair<String?, String>>, selected: String?, onPick: (String?) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        options.forEach { (id, label) ->
            WizardChip(label = label, selected = selected == id) { onPick(id) }
        }
    }
}

// ── Review ───────────────────────────────────────────────────────────────────

@Composable
private fun PfReviewStep(
    name: String,
    level: Int,
    ancestry: PfAncestry?,
    heritage: String?,
    background: PfBackground?,
    cls: PfClass?,
    subclassName: String?,
    keyAbility: PfAbility?,
    payload: PathfinderPayload,
    onEdit: (PfWiz) -> Unit,
    isActive: (PfWiz) -> Boolean,
) {
    val palette = MaterialTheme.natPalette
    // Pencil-jumps appear only for steps that are actually in the flow.
    fun edit(target: PfWiz): (() -> Unit)? = if (isActive(target)) ({ onEdit(target) }) else null
    val ancestryLine = listOfNotNull(heritage?.slugToTitle(), ancestry?.name).joinToString(" ")
    val classLine = listOfNotNull(subclassName, cls?.name).joinToString(" ").ifEmpty { "—" } + " $level"
    PfStepColumn {
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
                if (ancestryLine.isNotEmpty()) {
                    Text(ancestryLine, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = palette.inkSoft, maxLines = 1)
                }
                Text(
                    classLine,
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = palette.inkSoft,
                    maxLines = 1,
                )
            }
        }

        ReviewSectionHeader("Identity", edit(PfWiz.NAME))
        ReviewLine("Name", name.trim())

        ReviewSectionHeader("Ancestry", edit(PfWiz.ANCESTRY))
        ReviewLine("Ancestry", ancestryLine)

        ReviewSectionHeader("Background", edit(PfWiz.BACKGROUND))
        ReviewLine("Background", background?.name.orEmpty())

        ReviewSectionHeader("Class", edit(PfWiz.CLASS))
        ReviewLine("Class", classLine)
        subclassName?.let { ReviewLine(cls?.subclassLabel ?: "Subclass", it) }
        ReviewLine("Key ability", keyAbility?.displayName.orEmpty())

        ReviewSectionHeader("Abilities", edit(PfWiz.ABILITIES))
        PfScoresGrid(payload.abilityScores)

        ReviewSectionHeader("Vitals")
        val perception = payload.perceptionBonus
        ReviewLine(
            "Vitals",
            "HP ${payload.maxHp} · AC ${payload.armorClass} · Perception ${if (perception >= 0) "+$perception" else "$perception"} · Class DC ${payload.classDcValue}",
        )
        payload.spellTradition?.let { ReviewLine("Magic", "Casts ${it.displayName.lowercase()} spells") }

        ReviewSectionHeader("Feats", edit(PfWiz.FEATS))
        ReviewLine("Feats", payload.feats.mapNotNull { id -> PfFeats.by(id)?.name }.joinToString(" · "))
        if (level > 1 && isActive(PfWiz.ADVANCEMENT)) {
            ReviewSectionHeader("Advancement", edit(PfWiz.ADVANCEMENT))
            ReviewLine("Levels 2–$level", "Boosts, skill increases, and feats applied")
        }
    }
}
