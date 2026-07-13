package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.pf2e.PathfinderBuilder
import au.com.evonet.nat20.pf2e.PathfinderCatalog
import au.com.evonet.nat20.pf2e.PathfinderRuleset
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.ui.slugToTitle
import java.time.Instant

/**
 * The Pathfinder 2e (Remaster) creation wizard (A22): Name → Ancestry (+heritage)
 * → Background → Class (+key ability) → Abilities (the boost/flaw build) →
 * Skills → Review. The Abilities step is where PF2e diverges hardest — scores are
 * built from ordered +2/+1-at-18 boosts, resolved live via [PathfinderBuilder].
 */
private enum class PfWiz(val title: String) {
    NAME("Name"), ANCESTRY("Ancestry"), BACKGROUND("Background"), CLASS("Class"),
    ABILITIES("Abilities"), SKILLS("Skills"), REVIEW("Review"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PathfinderWizardScreen(
    onSave: (Character) -> Unit,
    onCancel: () -> Unit,
    stepOffset: Int = 0,
    onExitFirstStep: (() -> Unit)? = null,
) {
    val steps = PfWiz.entries
    var stepIndex by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var ancestryId by remember { mutableStateOf<String?>(null) }
    var heritage by remember { mutableStateOf<String?>(null) }
    var backgroundId by remember { mutableStateOf<String?>(null) }
    var classId by remember { mutableStateOf<String?>(null) }
    var keyAbility by remember { mutableStateOf<PfAbility?>(null) }
    var ancestryFree by remember { mutableStateOf<List<PfAbility>>(emptyList()) }
    var backgroundBoost by remember { mutableStateOf<PfAbility?>(null) }
    var backgroundFree by remember { mutableStateOf<PfAbility?>(null) }
    var freeBoosts by remember { mutableStateOf<List<PfAbility>>(emptyList()) }
    var chosenSkills by remember { mutableStateOf<Set<PfSkill>>(emptySet()) }

    val ancestry = ancestryId?.let(PathfinderCatalog::ancestry)
    val background = backgroundId?.let(PathfinderCatalog::background)
    val cls = classId?.let(PathfinderCatalog::pfClass)
    val step = steps[stepIndex]

    fun choices() = PathfinderBuilder.Choices(
        name = name, ancestryId = ancestryId.orEmpty(), heritage = heritage, backgroundId = backgroundId.orEmpty(),
        classId = classId.orEmpty(), keyAbility = keyAbility ?: PfAbility.STRENGTH,
        ancestryFreeBoosts = ancestryFree, backgroundBoost = backgroundBoost, backgroundFreeBoost = backgroundFree,
        freeBoosts = freeBoosts, chosenSkills = chosenSkills.toList(),
    )

    val skillSlots = if (cls != null && keyAbility != null) PathfinderBuilder.skillSlots(choices()) else 0

    fun canAdvance(): Boolean = when (step) {
        PfWiz.NAME -> name.isNotBlank()
        PfWiz.ANCESTRY -> ancestry != null && (ancestry.heritages.isEmpty() || heritage != null) && ancestryFree.size == ancestry.freeBoosts
        PfWiz.BACKGROUND -> background != null && backgroundBoost != null && backgroundFree != null
        PfWiz.CLASS -> cls != null && keyAbility != null
        PfWiz.ABILITIES -> freeBoosts.size == 4
        PfWiz.SKILLS -> chosenSkills.size == skillSlots
        PfWiz.REVIEW -> true
    }

    EditorShell(
        kicker = "Step ${stepOffset + stepIndex + 1} of ${stepOffset + steps.size}",
        title = "New Character",
        stepCount = stepOffset + steps.size,
        currentIndex = stepOffset + stepIndex,
        onBack = {
            when {
                stepIndex > 0 -> stepIndex--
                onExitFirstStep != null -> onExitFirstStep()
                else -> onCancel()
            }
        },
        onJump = { target ->
            if (target < stepOffset) {
                onExitFirstStep?.invoke()
            } else {
                stepIndex = (target - stepOffset).coerceIn(0, steps.lastIndex)
            }
        },
        scrollableContent = false, // the step area below scrolls itself (as it did pre-shell)
        footer = {
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
        },
    ) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (step) {
                    PfWiz.NAME -> IdentityStep(name, { name = it })
                    PfWiz.ANCESTRY -> {
                        PickRow(PathfinderCatalog.ancestries.map { it.id to it.name }, ancestryId) { ancestryId = it; heritage = null; ancestryFree = emptyList() }
                        ancestry?.let { a ->
                            Text("HP ${a.hp} · ${a.size} · Speed ${a.speed} ft" + (a.flaw?.let { " · −2 ${it.abbreviation}" } ?: "") + (a.boosts.takeIf { it.isNotEmpty() }?.let { " · +2 " + it.joinToString(",") { b -> b.abbreviation } } ?: ""), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            if (a.heritages.isNotEmpty()) { PfLabel("Heritage"); PickRow(a.heritages.map { it to it.slugToTitle() }, heritage) { heritage = it } }
                            PfLabel("Free ability boosts (${ancestryFree.size}/${a.freeBoosts})")
                            BoostGroup(ancestryFree, a.freeBoosts) { ancestryFree = it }
                        }
                    }
                    PfWiz.BACKGROUND -> {
                        PickRow(PathfinderCatalog.backgrounds.map { it.id to it.name }, backgroundId) { backgroundId = it; backgroundBoost = null; backgroundFree = null }
                        background?.let { b ->
                            Text("Trained: ${b.trainedSkill.displayName} · ${b.lore}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            PfLabel("Ability boost (choose one)")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { b.boostOptions.forEach { a -> FilterChip(backgroundBoost == a, { backgroundBoost = a }, label = { Text(a.abbreviation) }) } }
                            PfLabel("Free boost")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { PfAbility.entries.forEach { a -> FilterChip(backgroundFree == a, { backgroundFree = a }, label = { Text(a.abbreviation) }) } }
                        }
                    }
                    PfWiz.CLASS -> {
                        PickRow(PathfinderCatalog.classes.map { it.id to it.name }, classId) { classId = it; keyAbility = PathfinderCatalog.pfClass(it)?.keyAbilityOptions?.singleOrNull() }
                        cls?.let { c ->
                            Text("HP ${c.hpPerLevel}/level · Class DC ${c.classDC.displayName}" + (c.tradition?.let { " · ${it.displayName}" } ?: ""), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            PfLabel("Key ability")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { c.keyAbilityOptions.forEach { a -> FilterChip(keyAbility == a, { keyAbility = a }, label = { Text(a.displayName) }) } }
                        }
                    }
                    PfWiz.ABILITIES -> {
                        val scores = PathfinderBuilder.scores(choices())
                        PfLabel("Resolved scores")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PfAbility.entries.forEach { a ->
                                val s = scores.score(a); val m = PfAbilityScores.modifier(s)
                                androidx.compose.material3.AssistChip(onClick = {}, label = { Text("${a.abbreviation} $s (${if (m >= 0) "+$m" else m})") })
                            }
                        }
                        PfLabel("Four free boosts (${freeBoosts.size}/4)")
                        BoostGroup(freeBoosts, 4) { freeBoosts = it }
                    }
                    PfWiz.SKILLS -> {
                        Text("Choose $skillSlots class skills (${chosenSkills.size}/$skillSlots).", style = MaterialTheme.typography.bodyLarge)
                        background?.let { Text("Background grants ${it.trainedSkill.displayName}.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        val options = (cls?.classSkills ?: PfSkill.entries).filter { it != background?.trainedSkill }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            options.forEach { s ->
                                val on = s in chosenSkills
                                FilterChip(on, { chosenSkills = if (on) chosenSkills - s else if (chosenSkills.size < skillSlots) chosenSkills + s else chosenSkills }, label = { Text(s.displayName) })
                            }
                        }
                    }
                    PfWiz.REVIEW -> {
                        val p = PathfinderBuilder.build(choices())
                        ReviewLine("Name", name)
                        ReviewLine("Ancestry", listOfNotNull(heritage?.slugToTitle(), ancestry?.name).joinToString(" "))
                        ReviewLine("Background", background?.name ?: "—")
                        ReviewLine("Class", "${cls?.name} · key ${keyAbility?.displayName}")
                        ReviewLine("Abilities", PfAbility.entries.joinToString(" ") { "${it.abbreviation} ${p.abilityScores.score(it)}" })
                        ReviewLine("HP", p.maxHp.toString())
                        ReviewLine("Skills", p.skills.keys.joinToString { it.displayName }.ifEmpty { "—" })
                    }
                }
            }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickRow(items: List<Pair<String, String>>, selected: String?, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (id, label) -> FilterChip(selected == id, { onPick(id) }, label = { Text(label) }) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoostGroup(selected: List<PfAbility>, limit: Int, onChange: (List<PfAbility>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PfAbility.entries.forEach { a ->
            val on = a in selected
            FilterChip(on, enabled = on || selected.size < limit, onClick = { onChange(if (on) selected - a else selected + a) }, label = { Text(a.abbreviation) })
        }
    }
}

@Composable
private fun PfLabel(text: String) = Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

@Composable
private fun ReviewLine(label: String, value: String) {
    Column { Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(value, style = MaterialTheme.typography.bodyLarge) }
}
