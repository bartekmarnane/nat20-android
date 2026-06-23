package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.Background
import au.com.evonet.nat20.dnd5e.CharacterClass
import au.com.evonet.nat20.dnd5e.ClassEntry
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.DnD5eRuleset
import au.com.evonet.nat20.dnd5e.FightingStyles
import au.com.evonet.nat20.dnd5e.Race
import au.com.evonet.nat20.dnd5e.withFullSpellSlots
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.CastingProgression
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.domain.Character
import java.time.Instant
import kotlin.math.max

/**
 * The multi-step D&D 5e creation/edit wizard (A8). Replaces the minimal A6
 * form. Steps: Name → Race → Class → Background → Abilities → Skills → Review,
 * reading the SRD catalogues (`DnD5eCatalog`) and materialising a draft into a
 * `Character`. Port of the iOS step-8 wizard (Ruleset step omitted — one
 * ruleset; Manner/Spells/Advancements deferred).
 */
private enum class WizStep(val title: String) {
    NAME("Name"), RACE("Race"), CLASS("Class"), BACKGROUND("Background"),
    ABILITIES("Abilities"), SKILLS("Skills"), FIGHTING_STYLE("Fighting Style"),
    SPELLS("Spells"), REVIEW("Review"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnD5eWizardScreen(existing: Character?, onSave: (Character) -> Unit, onCancel: () -> Unit) {
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
    val steps = WizStep.entries.filter {
        (it != WizStep.FIGHTING_STYLE || grantsStyle) && (it != WizStep.SPELLS || castsAtCreation)
    }
    val step = steps[stepIndex.coerceIn(0, steps.lastIndex)]

    fun canAdvance(): Boolean = when (step) {
        WizStep.NAME -> name.isNotBlank()
        WizStep.RACE -> race != null
        WizStep.CLASS -> klass != null
        WizStep.BACKGROUND -> background != null
        WizStep.ABILITIES -> true
        WizStep.SKILLS -> klass != null && chosenSkills.size == klass.skillChoiceCount
        WizStep.FIGHTING_STYLE -> fightingStyle != null
        WizStep.SPELLS -> true
        WizStep.REVIEW -> true
    }

    fun build(): Character {
        val k = klass!!
        val conMod = AbilityScores.modifier(finalScores.constitution)
        val maxHp = LevelUpMath.firstLevelHp(k.hitDie, conMod) +
            LevelUpMath.averageGain(k.hitDie) * (level - 1)
        // Spell picks (A11): cantrips to cantripsKnown; 1st-level spells to the prepared or known list per class.
        val cantrips = if (castsAtCreation) chosenCantrips.toList() else emptyList()
        val spellList = if (castsAtCreation) chosenSpells.toList() else emptyList()
        val prepares = CastingProgression.usesPreparation(k.id)
        val payload = DnD5ePayload(
            race = raceId.orEmpty(),
            classes = listOf(ClassEntry(k.id, level)),
            abilityScores = finalScores,
            maxHp = maxHp,
            currentHp = maxHp,
            background = backgroundId.orEmpty(),
            selectedSkills = (backgroundSkills + chosenSkills).distinct(),
            fightingStyles = listOfNotNull(fightingStyle.takeIf { grantsStyle }),
            cantripsKnown = cantrips,
            spellsKnown = if (spellList.isNotEmpty() && !prepares) mapOf(k.id to spellList) else emptyMap(),
            preparedSpells = if (spellList.isNotEmpty() && prepares) mapOf(k.id to spellList) else emptyMap(),
        ).withFullSpellSlots() // casters start the day with all slots
        return if (existing == null) {
            Character.new(name.trim(), DnD5eRuleset(), payload, Instant.now())
        } else {
            existing.copy(name = name.trim(), payload = payload, updatedAt = Instant.now())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (existing == null) "New Character" else "Edit Character")
                        Text(
                            "Step ${stepIndex + 1} of ${steps.size} · ${step.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
            )
        },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxWidth()) {
                when (step) {
                    WizStep.NAME -> IdentityStep(name, { name = it }, Modifier.fillMaxSize().padding(16.dp))
                    WizStep.RACE -> PickStep(DnD5eCatalog.races, raceId, { it.id }, { it.name }, { it.description }) { raceId = it }
                    WizStep.CLASS -> ClassStep(classId, level, onPick = { classId = it; chosenSkills = emptySet() }, onLevel = { level = it })
                    WizStep.BACKGROUND -> PickStep(DnD5eCatalog.backgrounds, backgroundId, { it.id }, { it.name }, { it.description }) { backgroundId = it }
                    WizStep.ABILITIES -> AbilitiesStep(base, raceBonus, finalScores) { base = it }
                    WizStep.SKILLS -> SkillsStep(klass, backgroundSkills, chosenSkills) { chosenSkills = it }
                    WizStep.FIGHTING_STYLE -> FightingStyleStep(fightingStyle) { fightingStyle = it }
                    WizStep.SPELLS -> SpellsStep(klass, chosenCantrips, chosenSpells, { chosenCantrips = it }, { chosenSpells = it })
                    WizStep.REVIEW -> ReviewStep(name, race, klass, level, background, finalScores, backgroundSkills + chosenSkills)
                }
            }
            NavBar(
                showBack = stepIndex > 0,
                isLast = step == WizStep.REVIEW,
                canAdvance = canAdvance(),
                onBack = { stepIndex-- },
                onNext = { stepIndex++ },
                onSave = { onSave(build()) },
            )
        }
    }
}

// ── Steps ────────────────────────────────────────────────────────────────────
// The NAME step uses the shared `IdentityStep` (see IdentityStep.kt).

@Composable
private fun <T> PickStep(
    items: List<T>,
    selectedId: String?,
    id: (T) -> String,
    title: (T) -> String,
    subtitle: (T) -> String,
    onPick: (String) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items, key = id) { item ->
            PickCard(
                title = title(item),
                subtitle = subtitle(item),
                selected = id(item) == selectedId,
                onClick = { onPick(id(item)) },
            )
        }
    }
}

@Composable
private fun PickCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ClassStep(classId: String?, level: Int, onPick: (String) -> Unit, onLevel: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Starting level", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Stepper(level, 1..20, onLevel)
        }
        PickStep(DnD5eCatalog.classes, classId, { it.id }, { it.name }, { "d${it.hitDie} · ${it.savingThrows.joinToString(", ")}" }, onPick)
    }
}

@Composable
private fun AbilitiesStep(base: AbilityScores, raceBonus: Map<Ability, Int>, finalScores: AbilityScores, onChange: (AbilityScores) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Set base scores; racial bonuses are added automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Ability.entries.forEach { ability ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(ability.abbreviation, Modifier.width(48.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                val bonus = raceBonus[ability] ?: 0
                val finalScore = finalScores.score(ability)
                Text(
                    if (bonus != 0) "→ $finalScore (${if (bonus > 0) "+$bonus" else "$bonus"})" else "→ $finalScore",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Stepper(base.score(ability), 3..20) { onChange(base.with(ability, it)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SkillsStep(klass: CharacterClass?, backgroundSkills: List<String>, chosen: Set<String>, onChange: (Set<String>) -> Unit) {
    val quota = klass?.skillChoiceCount ?: 0
    val options = (klass?.skillChoiceFrom.orEmpty()).filterNot { it in backgroundSkills }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Choose $quota class skills (${chosen.size}/$quota).", style = MaterialTheme.typography.bodyLarge)
        if (backgroundSkills.isNotEmpty()) {
            Text("From your background: ${backgroundSkills.joinToString { skillName(it) }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { skillId ->
                val isChosen = skillId in chosen
                FilterChip(
                    selected = isChosen,
                    onClick = {
                        onChange(if (isChosen) chosen - skillId else if (chosen.size < quota) chosen + skillId else chosen)
                    },
                    label = { Text(skillName(skillId)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpellsStep(
    klass: CharacterClass?,
    cantrips: Set<String>,
    spells: Set<String>,
    onCantrips: (Set<String>) -> Unit,
    onSpells: (Set<String>) -> Unit,
) {
    val className = klass?.name ?: return
    // Filter the SRD library to this class's cantrips and 1st-level spells.
    val cantripPool = remember(className) { DnD5eCatalog.spellLibrary.filter { it.level == 0 && it.classNames.any { c -> c.equals(className, ignoreCase = true) } }.sortedBy { it.name } }
    val spellPool = remember(className) { DnD5eCatalog.spellLibrary.filter { it.level == 1 && it.classNames.any { c -> c.equals(className, ignoreCase = true) } }.sortedBy { it.name } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Choose your starting spells.", style = MaterialTheme.typography.bodyLarge)
        Text("Cantrips (${cantrips.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cantripPool.forEach { s ->
                val on = s.index in cantrips
                FilterChip(selected = on, onClick = { onCantrips(if (on) cantrips - s.index else cantrips + s.index) }, label = { Text(s.name) })
            }
        }
        Text("1st-level spells (${spells.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            spellPool.forEach { s ->
                val on = s.index in spells
                FilterChip(selected = on, onClick = { onSpells(if (on) spells - s.index else spells + s.index) }, label = { Text(s.name) })
            }
        }
        Text("You can add or change spells anytime on the Spells tab.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FightingStyleStep(selected: String?, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Choose your Fighting Style.", style = MaterialTheme.typography.bodyLarge)
        FightingStyles.all.forEach { fs ->
            PickCard(title = fs.name, subtitle = fs.description, selected = selected == fs.id) { onPick(fs.id) }
        }
    }
}

@Composable
private fun ReviewStep(name: String, race: Race?, klass: CharacterClass?, level: Int, background: Background?, scores: AbilityScores, skills: List<String>) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReviewLine("Name", name)
        ReviewLine("Race", race?.name ?: "—")
        ReviewLine("Class", klass?.let { "${it.name} $level" } ?: "—")
        ReviewLine("Background", background?.name ?: "—")
        ReviewLine("Abilities", Ability.entries.joinToString(" ") { "${it.abbreviation} ${scores.score(it)}" })
        ReviewLine("Skills", skills.joinToString { skillName(it) }.ifEmpty { "—" })
    }
}

@Composable
private fun ReviewLine(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

// ── Shared ───────────────────────────────────────────────────────────────────

@Composable
private fun NavBar(showBack: Boolean, isLast: Boolean, canAdvance: Boolean, onBack: () -> Unit, onNext: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.weight(1f))
        if (isLast) {
            Button(onClick = onSave, enabled = canAdvance) { Text("Save") }
        } else {
            Button(onClick = onNext, enabled = canAdvance) { Text("Next") }
        }
    }
}

@Composable
private fun Stepper(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { if (value > range.first) onChange(value - 1) },
            enabled = value > range.first,
            modifier = Modifier.size(40.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) { Text("−") }
        Text(value.toString(), Modifier.width(40.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        OutlinedButton(
            onClick = { if (value < range.last) onChange(value + 1) },
            enabled = value < range.last,
            modifier = Modifier.size(40.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) { Text("+") }
    }
}

private fun skillName(id: String): String = DnD5eCatalog.skill(id)?.name ?: id

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
