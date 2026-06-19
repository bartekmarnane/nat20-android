package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.dnd5e2024.ClassEntry2024
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Catalog
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Ruleset
import au.com.evonet.nat20.dnd5e2024.FeatCategory2024
import au.com.evonet.nat20.dnd5e2024.Feats2024
import au.com.evonet.nat20.dnd5e2024.effectiveMaxHp
import au.com.evonet.nat20.dnd5e2024.withFullSpellSlots
import au.com.evonet.nat20.domain.Character
import java.time.Instant

/**
 * The D&D 5e (2024) creation wizard (A21): Name → Species → Class → Background →
 * Abilities → Skills → Review. The 2024 differences are here — **species carry
 * no ability bumps** (traits only) and **the background grants the +2/+1 ASI**
 * (allocated in the Abilities step) plus an origin feat. Spell selection happens
 * on the Spells tab; subclass is chosen at level 3.
 */
private enum class Wiz2024(val title: String) {
    NAME("Name"), SPECIES("Species"), CLASS("Class"), BACKGROUND("Background"),
    ABILITIES("Abilities"), SKILLS("Skills"), ADVANCEMENTS("Advancements"), REVIEW("Review"),
}

private enum class AsiMode2024 { TWO_ONE, ONE_EACH }

/** A single advancement-level choice in the above-level-1 creation step. */
private enum class AdvKind2024 { ASI_ONE, ASI_TWO, FEAT }

private data class AdvState2024(
    val kind: AdvKind2024 = AdvKind2024.ASI_ONE,
    val abilities: List<Ability> = emptyList(),
    val featId: String? = null,
    val half: Ability? = null,
) {
    /** The ability bumps this choice contributes (+2/one, +1/two, or a half-feat's +1). */
    fun bumps(): Map<Ability, Int> = when (kind) {
        AdvKind2024.ASI_ONE -> abilities.take(1).associateWith { 2 }
        AdvKind2024.ASI_TWO -> abilities.take(2).associateWith { 1 }
        AdvKind2024.FEAT -> half?.let { mapOf(it to 1) }.orEmpty()
    }

    val chosenFeat: String? get() = featId.takeIf { kind == AdvKind2024.FEAT }

    fun isComplete(): Boolean = when (kind) {
        AdvKind2024.ASI_ONE -> abilities.size == 1
        AdvKind2024.ASI_TWO -> abilities.size == 2
        AdvKind2024.FEAT -> featId != null && (Feats2024.feat(featId)?.grantsAbilityIncrease != true || half != null)
    }
}

/** Levels at or below [level] where [classId] grants an Ability Score Improvement (the advancement levels). */
private fun asiLevels2024(classId: String?, level: Int): List<Int> =
    if (classId == null) emptyList() else (1..level).filter { LevelUpMath.grantsAbilityScoreImprovement(classId, it) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnD5e2024WizardScreen(onSave: (Character) -> Unit, onCancel: () -> Unit) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var speciesId by rememberSaveable { mutableStateOf<String?>(null) }
    var classId by rememberSaveable { mutableStateOf<String?>(null) }
    var level by rememberSaveable { mutableIntStateOf(1) }
    var backgroundId by rememberSaveable { mutableStateOf<String?>(null) }
    var base by remember { mutableStateOf(AbilityScores()) }
    var asiMode by rememberSaveable { mutableStateOf(AsiMode2024.TWO_ONE) }
    var plus2 by remember { mutableStateOf<Ability?>(null) }
    var plus1 by remember { mutableStateOf<Ability?>(null) }
    var chosenSkills by remember { mutableStateOf(emptySet<String>()) }
    var subclass by remember { mutableStateOf<String?>(null) }
    var advs by remember { mutableStateOf<Map<Int, AdvState2024>>(emptyMap()) }

    val klass = classId?.let(DnD5e2024Catalog::characterClass)
    val background = backgroundId?.let(DnD5e2024Catalog::background)
    val backgroundSkills = background?.skills.orEmpty()
    val asi = backgroundAsi(asiMode, background, plus2, plus1)
    val finalScores = base.applying(asi)

    // Above level 1, an Advancements step captures the subclass + each ASI-level choice.
    val asiLevels = asiLevels2024(classId, level)
    val needsSubclass = klass != null && level >= klass.subclassLevel
    val needsAdvancements = klass != null && (needsSubclass || asiLevels.isNotEmpty())
    val steps = Wiz2024.entries.filter { it != Wiz2024.ADVANCEMENTS || needsAdvancements }
    val step = steps[stepIndex.coerceIn(0, steps.lastIndex)]

    fun canAdvance(): Boolean = when (step) {
        Wiz2024.NAME -> name.isNotBlank()
        Wiz2024.SPECIES -> speciesId != null
        Wiz2024.CLASS -> klass != null
        Wiz2024.BACKGROUND -> background != null
        Wiz2024.ABILITIES -> asi.values.sum() == 3 // a complete +2/+1 or +1/+1/+1 allocation
        Wiz2024.SKILLS -> klass != null && chosenSkills.size == klass.skillChoiceCount
        Wiz2024.ADVANCEMENTS -> (!needsSubclass || subclass != null) && asiLevels.all { (advs[it] ?: AdvState2024()).isComplete() }
        Wiz2024.REVIEW -> true
    }

    fun build(): Character {
        val k = klass!!
        // Fold the advancement ASIs/half-feats into the scores (capped at 20) + collect chosen feats.
        var scores = finalScores
        val feats = mutableListOf<String>()
        for (lvl in asiLevels) {
            val choice = advs[lvl] ?: AdvState2024()
            for ((ability, inc) in choice.bumps()) scores = scores.with(ability, minOf(20, scores.score(ability) + inc))
            choice.chosenFeat?.let { feats += it }
        }
        val conMod = AbilityScores.modifier(scores.constitution)
        val maxHp = LevelUpMath.firstLevelHp(k.hitDie, conMod) + LevelUpMath.averageGain(k.hitDie) * (level - 1)
        val payload = DnD5e2024Payload(
            species = speciesId.orEmpty(),
            classes = listOf(ClassEntry2024(k.id, level, subclass = subclass.takeIf { needsSubclass })),
            background = backgroundId.orEmpty(),
            abilityScores = scores,
            baseAbilityScores = base,
            backgroundASI = asi,
            maxHp = maxHp,
            skillProficiencies = (backgroundSkills + chosenSkills).distinct(),
            originFeat = background?.originFeat,
            chosenFeats = feats.distinct(),
        ).withFullSpellSlots().let { it.copy(currentHp = it.effectiveMaxHp) } // start at full incl. Tough / Dwarven Toughness
        return Character.new(name.trim(), DnD5e2024Ruleset(), payload, Instant.now())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New Character · 2024")
                        Text("Step ${stepIndex + 1} of ${steps.size} · ${step.title}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
            )
        },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxWidth()) {
                when (step) {
                    Wiz2024.NAME -> Name2024(name) { name = it }
                    Wiz2024.SPECIES -> Pick2024(DnD5e2024Catalog.species, speciesId, { it.id }, { it.name }, { it.description }) { speciesId = it }
                    Wiz2024.CLASS -> Class2024(classId, level, onPick = { classId = it; chosenSkills = emptySet() }, onLevel = { level = it })
                    Wiz2024.BACKGROUND -> Pick2024(DnD5e2024Catalog.backgrounds, backgroundId, { it.id }, { it.name }, { "${it.description} · ${it.abilityOptions.joinToString(", ") { a -> a.abbreviation }}" }) { backgroundId = it; plus2 = null; plus1 = null }
                    Wiz2024.ABILITIES -> Abilities2024(base, background, asiMode, plus2, plus1, finalScores, onBase = { base = it }, onMode = { asiMode = it; plus2 = null; plus1 = null }, onPlus2 = { plus2 = it; if (plus1 == it) plus1 = null }, onPlus1 = { plus1 = it })
                    Wiz2024.SKILLS -> Skills2024(klass, backgroundSkills, chosenSkills) { chosenSkills = it }
                    Wiz2024.ADVANCEMENTS -> Advancements2024(klass, level, needsSubclass, asiLevels, finalScores, subclass, advs, onSubclass = { subclass = it }, onAdv = { lvl, s -> advs = advs + (lvl to s) })
                    Wiz2024.REVIEW -> Review2024(name, speciesId, klass, level, background, finalScores, backgroundSkills + chosenSkills)
                }
            }
            Nav2024(showBack = stepIndex > 0, isLast = step == Wiz2024.REVIEW, canAdvance = canAdvance(), onBack = { stepIndex-- }, onNext = { stepIndex++ }, onSave = { onSave(build()) })
        }
    }
}

// ── Steps ──────────────────────────────────────────────────────────────────────

@Composable
private fun Name2024(name: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(name, onChange, label = { Text("Character name") }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun <T> Pick2024(items: List<T>, selectedId: String?, id: (T) -> String, title: (T) -> String, subtitle: (T) -> String, onPick: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items, key = id) { item ->
            Card(
                Modifier.fillMaxWidth().clickable { onPick(id(item)) },
                colors = if (id(item) == selectedId) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) else CardDefaults.cardColors(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title(item), style = MaterialTheme.typography.titleMedium)
                    Text(subtitle(item), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun Class2024(classId: String?, level: Int, onPick: (String) -> Unit, onLevel: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Starting level", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Stepper2024(level, 1..20, onLevel)
        }
        Pick2024(DnD5e2024Catalog.classes, classId, { it.id }, { it.name }, { "d${it.hitDie} · ${it.savingThrows.joinToString(", ") { a -> a.abbreviation }}" }, onPick)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Abilities2024(
    base: AbilityScores,
    background: au.com.evonet.nat20.dnd5e2024.Background2024?,
    mode: AsiMode2024,
    plus2: Ability?,
    plus1: Ability?,
    finalScores: AbilityScores,
    onBase: (AbilityScores) -> Unit,
    onMode: (AsiMode2024) -> Unit,
    onPlus2: (Ability) -> Unit,
    onPlus1: (Ability) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Set base scores; your background adds the ability bonuses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Ability.entries.forEach { ability ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(ability.abbreviation, Modifier.width(48.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("→ ${finalScores.score(ability)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Stepper2024(base.score(ability), 3..20) { onBase(base.with(ability, it)) }
            }
        }
        if (background == null) {
            Text("Pick a background first to allocate its ability bonuses.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        } else {
            Text("${background.name} bonuses — across ${background.abilityOptions.joinToString(", ") { it.abbreviation }}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(mode == AsiMode2024.TWO_ONE, { onMode(AsiMode2024.TWO_ONE) }, label = { Text("+2 / +1") })
                FilterChip(mode == AsiMode2024.ONE_EACH, { onMode(AsiMode2024.ONE_EACH) }, label = { Text("+1 / +1 / +1") })
            }
            if (mode == AsiMode2024.TWO_ONE) {
                Text("+2 to:", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    background.abilityOptions.forEach { a -> FilterChip(plus2 == a, { onPlus2(a) }, label = { Text(a.abbreviation) }) }
                }
                Text("+1 to:", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    background.abilityOptions.filter { it != plus2 }.forEach { a -> FilterChip(plus1 == a, { onPlus1(a) }, label = { Text(a.abbreviation) }) }
                }
            } else {
                Text("+1 to each of ${background.abilityOptions.joinToString(", ") { it.abbreviation }}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Skills2024(klass: CharacterClass2024Ui?, backgroundSkills: List<String>, chosen: Set<String>, onChange: (Set<String>) -> Unit) {
    val quota = klass?.skillChoiceCount ?: 0
    val options = (klass?.skillChoiceFrom.orEmpty()).filterNot { it in backgroundSkills }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Choose $quota class skills (${chosen.size}/$quota).", style = MaterialTheme.typography.bodyLarge)
        if (backgroundSkills.isNotEmpty()) Text("From your background: ${backgroundSkills.joinToString { skill2024(it) }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { skillId ->
                val isChosen = skillId in chosen
                FilterChip(isChosen, { onChange(if (isChosen) chosen - skillId else if (chosen.size < quota) chosen + skillId else chosen) }, label = { Text(skill2024(skillId)) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Advancements2024(
    klass: CharacterClass2024Ui?,
    level: Int,
    needsSubclass: Boolean,
    asiLevels: List<Int>,
    scores: AbilityScores,
    subclass: String?,
    advs: Map<Int, AdvState2024>,
    onSubclass: (String) -> Unit,
    onAdv: (Int, AdvState2024) -> Unit,
) {
    klass ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your ${klass.name} is level $level — make the choices earned along the way.", style = MaterialTheme.typography.bodyLarge)

        if (needsSubclass && klass.subclasses.isNotEmpty()) {
            StepLabel2024("Subclass (level ${klass.subclassLevel})")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                klass.subclasses.forEach { s -> FilterChip(subclass == s.id, { onSubclass(s.id) }, label = { Text(s.name) }) }
            }
        }

        asiLevels.forEach { lvl ->
            val state = advs[lvl] ?: AdvState2024()
            StepLabel2024("Level $lvl — Ability Score Improvement or Feat")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.kind == AdvKind2024.ASI_ONE, { onAdv(lvl, AdvState2024(AdvKind2024.ASI_ONE)) }, label = { Text("+2 one") })
                FilterChip(state.kind == AdvKind2024.ASI_TWO, { onAdv(lvl, AdvState2024(AdvKind2024.ASI_TWO)) }, label = { Text("+1 two") })
                FilterChip(state.kind == AdvKind2024.FEAT, { onAdv(lvl, AdvState2024(AdvKind2024.FEAT)) }, label = { Text("Feat") })
            }
            when (state.kind) {
                AdvKind2024.ASI_ONE, AdvKind2024.ASI_TWO -> {
                    val limit = if (state.kind == AdvKind2024.ASI_ONE) 1 else 2
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Ability.entries.forEach { a ->
                            val picked = a in state.abilities
                            FilterChip(
                                picked,
                                enabled = scores.score(a) < 20 && (picked || state.abilities.size < limit),
                                onClick = { onAdv(lvl, state.copy(abilities = if (picked) state.abilities - a else state.abilities + a)) },
                                label = { Text("${a.abbreviation} ${scores.score(a)}") },
                            )
                        }
                    }
                }
                AdvKind2024.FEAT -> {
                    val available = Feats2024.inCategory(FeatCategory2024.GENERAL).filter { it.id != "ability-score-improvement" && it.isAvailable(lvl, scores, klass.isCaster) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        available.forEach { f -> FilterChip(state.featId == f.id, { onAdv(lvl, state.copy(featId = f.id, half = null)) }, label = { Text(f.name) }) }
                    }
                    state.featId?.let { Feats2024.feat(it) }?.let { f ->
                        Text(f.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (f.grantsAbilityIncrease) {
                            Text("+1 ability score:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Ability.entries.forEach { a -> FilterChip(state.half == a, enabled = scores.score(a) < 20, onClick = { onAdv(lvl, state.copy(half = a)) }, label = { Text("${a.abbreviation} ${scores.score(a)}") }) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepLabel2024(text: String) =
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

@Composable
private fun Review2024(name: String, speciesId: String?, klass: CharacterClass2024Ui?, level: Int, background: au.com.evonet.nat20.dnd5e2024.Background2024?, scores: AbilityScores, skills: List<String>) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Review2024Line("Name", name)
        Review2024Line("Species", speciesId?.let { DnD5e2024Catalog.species(it)?.name } ?: "—")
        Review2024Line("Class", klass?.let { "${it.name} $level" } ?: "—")
        Review2024Line("Background", background?.let { "${it.name} · ${it.originFeat.let(::titleCase)} (origin feat)" } ?: "—")
        Review2024Line("Abilities", Ability.entries.joinToString(" ") { "${it.abbreviation} ${scores.score(it)}" })
        Review2024Line("Skills", skills.joinToString { skill2024(it) }.ifEmpty { "—" })
    }
}

@Composable
private fun Review2024Line(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

// ── Shared atoms ─────────────────────────────────────────────────────────────

@Composable
private fun Nav2024(showBack: Boolean, isLast: Boolean, canAdvance: Boolean, onBack: () -> Unit, onNext: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.weight(1f))
        if (isLast) Button(onClick = onSave, enabled = canAdvance) { Text("Save") } else Button(onClick = onNext, enabled = canAdvance) { Text("Next") }
    }
}

@Composable
private fun Stepper2024(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { if (value > range.first) onChange(value - 1) }, enabled = value > range.first, modifier = Modifier.size(40.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("−") }
        Text(value.toString(), Modifier.width(40.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        OutlinedButton(onClick = { if (value < range.last) onChange(value + 1) }, enabled = value < range.last, modifier = Modifier.size(40.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("+") }
    }
}

private typealias CharacterClass2024Ui = au.com.evonet.nat20.dnd5e2024.CharacterClass2024

private fun skill2024(id: String): String = DnD5eCatalog.skill(id)?.name ?: id
private fun titleCase(slug: String): String = slug.split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

private fun backgroundAsi(mode: AsiMode2024, background: au.com.evonet.nat20.dnd5e2024.Background2024?, plus2: Ability?, plus1: Ability?): Map<Ability, Int> {
    background ?: return emptyMap()
    return when (mode) {
        AsiMode2024.ONE_EACH -> background.abilityOptions.associateWith { 1 }
        AsiMode2024.TWO_ONE -> buildMap {
            if (plus2 != null) put(plus2, 2)
            if (plus1 != null && plus1 != plus2) put(plus1, 1)
        }
    }
}

private fun AbilityScores.applying(bonus: Map<Ability, Int>): AbilityScores =
    Ability.entries.fold(this) { acc, ability -> acc.with(ability, acc.score(ability) + (bonus[ability] ?: 0)) }
