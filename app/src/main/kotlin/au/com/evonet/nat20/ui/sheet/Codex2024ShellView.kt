package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.CastingProgression
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e2024.ApplyCondition2024
import au.com.evonet.nat20.dnd5e2024.CancelEffect2024
import au.com.evonet.nat20.dnd5e2024.CastSpell2024
import au.com.evonet.nat20.dnd5e2024.ClearCondition2024
import au.com.evonet.nat20.dnd5e2024.ChangeExhaustion2024
import au.com.evonet.nat20.dnd5e2024.EndConcentration2024
import au.com.evonet.nat20.dnd5e2024.armorClass
import au.com.evonet.nat20.dnd5e2024.temporarySaveBonus
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Catalog
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.dnd5e2024.ExpendSpellSlot2024
import au.com.evonet.nat20.dnd5e2024.Exhaustion2024
import au.com.evonet.nat20.dnd5e2024.GainTempHp2024
import au.com.evonet.nat20.dnd5e2024.Heal2024
import au.com.evonet.nat20.dnd5e2024.LongRest2024
import au.com.evonet.nat20.dnd5e2024.RollDeathSave2024
import au.com.evonet.nat20.dnd5e2024.SetInitiative2024
import au.com.evonet.nat20.dnd5e2024.SetInspiration2024
import au.com.evonet.nat20.dnd5e2024.Spell2024
import au.com.evonet.nat20.dnd5e2024.SpendHitDie2024
import au.com.evonet.nat20.dnd5e2024.TakeDamage2024
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.roll.RollDialog
import au.com.evonet.nat20.ui.slugToTitle
import kotlinx.coroutines.launch

/**
 * The interactive D&D 5e (2024) codex (A21): a swipeable Stats / Skills /
 * Combat / Spells / Lore shell typed to [DnD5e2024Payload]. It reuses the shared
 * `-core` machinery, the `:app` die-roll primitive, and the 2014 skill list
 * (skills are edition-agnostic), and journals through the same `onApplyIntent`.
 * Inventory + weapon-mastery + the full feat system are follow-up slices.
 */
private enum class Tab2024(val title: String) { STATS("Stats"), SKILLS("Skills"), COMBAT("Combat"), SPELLS("Spells"), LORE("Lore") }

@Composable
fun Codex2024ShellView(
    character: Character,
    onApplyIntent: (CharacterIntent) -> Unit,
    onSave: (Character) -> Unit,
    modifier: Modifier = Modifier,
) {
    val payload = character.payload as? DnD5e2024Payload ?: return
    val tabs = Tab2024.entries
    val pager = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    var levelingUp by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(character.name, style = MaterialTheme.typography.headlineSmall)
            Text(heroLine2024(payload), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("5th Edition (2024)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            when (tabs[page]) {
                Tab2024.STATS -> Stats2024(payload) { levelingUp = true }
                Tab2024.SKILLS -> Skills2024(payload)
                Tab2024.COMBAT -> Combat2024(character, payload, onApplyIntent)
                Tab2024.SPELLS -> Spells2024(character, payload, onApplyIntent, onSave)
                Tab2024.LORE -> Lore2024(character, payload)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { i, tab ->
                val active = i == pager.currentPage
                Text(
                    tab.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).clickable { scope.launch { pager.animateScrollToPage(i) } }.padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }

    if (levelingUp) {
        LevelUp2024Wizard(payload, onApplyIntent, onDismiss = { levelingUp = false })
    }
}

// ── Tabs ──────────────────────────────────────────────────────────────────────

@Composable
private fun Stats2024(payload: DnD5e2024Payload, onLevelUp: () -> Unit) {
    val scores = payload.effectiveAbilityScores
    val prof = Proficiency.bonus(payload.level)
    var check by remember { mutableStateOf<Pair<String, List<RollBonus>>?>(null) }
    Page2024 {
        Card2024("Abilities") {
            Ability.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { ability ->
                        val mod = AbilityScores.modifier(scores.score(ability))
                        Medallion2024(ability.abbreviation, scores.score(ability), Modifier.weight(1f).clickable { check = "${ability.abbreviation} check" to listOf(RollBonus(ability.abbreviation, mod)) })
                    }
                }
            }
        }
        Card2024("Proficiency & Senses") {
            StatRow("Proficiency Bonus", prof.signed2024())
            StatRow("Passive Perception", (10 + AbilityScores.modifier(scores.score(Ability.WISDOM)) + if ("perception" in payload.skillProficiencies) prof else 0).toString())
        }
        Card2024("Saving Throws") {
            Ability.entries.forEach { a ->
                val abilityMod = AbilityScores.modifier(scores.score(a))
                val effectBonus = payload.temporarySaveBonus(a)
                val mod = abilityMod + effectBonus
                Row(Modifier.fillMaxWidth().clickable {
                    check = "${a.abbreviation} save" to buildList { add(RollBonus(a.abbreviation, abilityMod)); if (effectBonus != 0) add(RollBonus("Effects", effectBonus)) }
                }.padding(vertical = 2.dp)) {
                    Text(a.abbreviation, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Text(mod.signed2024(), fontWeight = FontWeight.Medium)
                }
            }
        }
        if (payload.level < DnD5e2024Payload.MAX_LEVEL) {
            Button(onClick = onLevelUp, modifier = Modifier.fillMaxWidth()) { Text("Level Up") }
        }
    }
    check?.let { (title, bonuses) -> RollDialog(title, RollSpec.d(1, 20), bonuses, onDismiss = { check = null }) }
}

@Composable
private fun Skills2024(payload: DnD5e2024Payload) {
    val prof = Proficiency.bonus(payload.level)
    val scores = payload.effectiveAbilityScores
    var check by remember { mutableStateOf<Pair<String, List<RollBonus>>?>(null) }
    Page2024 {
        Card2024("Skills") {
            DnD5eCatalog.skills.forEachIndexed { i, skill ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                val proficient = skill.id in payload.skillProficiencies
                val abilityMod = AbilityScores.modifier(scores.score(skill.ability))
                val mod = abilityMod + if (proficient) prof else 0
                Row(
                    Modifier.fillMaxWidth().clickable {
                        check = "${skill.name} check" to buildList { add(RollBonus(skill.ability.abbreviation, abilityMod)); if (proficient) add(RollBonus("Proficiency", prof)) }
                    }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (proficient) "●" else "○", color = if (proficient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                    Text(skill.name, Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                    Text(skill.ability.abbreviation, Modifier.padding(end = 12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(mod.signed2024(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    check?.let { (title, bonuses) -> RollDialog(title, RollSpec.d(1, 20), bonuses, onDismiss = { check = null }) }
}

@Composable
private fun Combat2024(character: Character, payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit) {
    val dexMod = AbilityScores.modifier(payload.effectiveScore(Ability.DEXTERITY))
    var amount by remember { mutableStateOf<AmountKind?>(null) }
    var rolling by remember { mutableStateOf<RollKind?>(null) }
    var addCond by remember { mutableStateOf(false) }
    Page2024 {
        Card2024("Hit Points") {
            StatRow("Current / Max", "${payload.currentHp} / ${payload.maxHp}" + if (payload.temporaryHp > 0) " (+${payload.temporaryHp})" else "")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { amount = AmountKind.DAMAGE }) { Text("Damage") }
                OutlinedButton(onClick = { amount = AmountKind.HEAL }) { Text("Heal") }
                OutlinedButton(onClick = { amount = AmountKind.TEMP }) { Text("Temp HP") }
            }
        }
        if (payload.currentHp == 0 || !payload.deathSaves.isCleared) {
            Card2024("Death Saves") {
                StatRow("Successes / Failures", "${payload.deathSaves.successes} / ${payload.deathSaves.failures}")
                if (!payload.deathSaves.isStable && !payload.deathSaves.isDead) {
                    Button(onClick = { rolling = RollKind.DEATH_SAVE }, modifier = Modifier.fillMaxWidth()) { Text("Roll a death save") }
                }
            }
        }
        Effects2024Card(payload, onApplyIntent)
        Card2024("Defense & Movement") {
            StatRow("Armor Class", payload.armorClass.toString())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Initiative", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Text(payload.initiative?.toString() ?: dexMod.signed2024(), fontWeight = FontWeight.Medium, color = if (payload.initiative != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = { rolling = RollKind.INITIATIVE }) { Text(if (payload.initiative != null) "Reroll" else "Roll") }
                if (payload.initiative != null) TextButton(onClick = { onApplyIntent(SetInitiative2024(null)) }) { Text("Clear") }
            }
        }
        Card2024("Hit Dice") {
            StatRow("Available", "${payload.currentHitDice} / ${payload.maxHitDice}")
            OutlinedButton(enabled = payload.currentHitDice > 0, onClick = { rolling = RollKind.HIT_DIE }) { Text("Spend a hit die") }
        }
        Card2024("Exhaustion & Inspiration") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Exhaustion ${payload.exhaustionLevel} / ${Exhaustion2024.MAX}", style = MaterialTheme.typography.bodyMedium)
                    if (payload.exhaustionLevel > 0) Text("${Exhaustion2024.d20Modifier(payload.exhaustionLevel)} to d20 tests · −${Exhaustion2024.speedPenaltyFeet(payload.exhaustionLevel)} ft", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(enabled = payload.exhaustionLevel > 0, onClick = { onApplyIntent(ChangeExhaustion2024(-1)) }) { Text("−") }
                TextButton(enabled = payload.exhaustionLevel < Exhaustion2024.MAX, onClick = { onApplyIntent(ChangeExhaustion2024(1)) }) { Text("+") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (payload.hasInspiration) "Holds Heroic Inspiration" else "No Heroic Inspiration", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = if (payload.hasInspiration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { onApplyIntent(SetInspiration2024(!payload.hasInspiration)) }) { Text(if (payload.hasInspiration) "Use" else "Grant") }
            }
        }
        Card2024("Conditions") {
            if (payload.activeConditions.isEmpty()) Text("No conditions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else FlowRowChips(payload.activeConditions) { onApplyIntent(ClearCondition2024(it)) }
            OutlinedButton(onClick = { addCond = true }) { Text("Add condition") }
        }
        Card2024("Rest") {
            OutlinedButton(onClick = { onApplyIntent(LongRest2024()) }) { Text("Long rest") }
        }
    }

    amount?.let { kind ->
        AmountDialog(
            title = kind.label,
            onConfirm = { n ->
                onApplyIntent(when (kind) { AmountKind.DAMAGE -> TakeDamage2024(n); AmountKind.HEAL -> Heal2024(n); AmountKind.TEMP -> GainTempHp2024(n) })
                amount = null
            },
            onDismiss = { amount = null },
        )
    }
    rolling?.let { kind ->
        val con = AbilityScores.modifier(payload.effectiveScore(Ability.CONSTITUTION))
        when (kind) {
            RollKind.DEATH_SAVE -> RollDialog("Death save", RollSpec.d(1, 20), allowAdvantageToggle = false, onSettled = { it.naturalD20?.let { d -> onApplyIntent(RollDeathSave2024(d)) } }, onDismiss = { rolling = null })
            RollKind.INITIATIVE -> RollDialog("Initiative", RollSpec.d(1, 20), bonuses = listOf(RollBonus("DEX", dexMod)), onSettled = { onApplyIntent(SetInitiative2024(it.total)) }, onDismiss = { rolling = null })
            RollKind.HIT_DIE -> RollDialog("Spend a hit die", RollSpec.d(1, hitDie2024(payload)), bonuses = if (con != 0) listOf(RollBonus("CON", con)) else emptyList(), allowAdvantageToggle = false, onSettled = { onApplyIntent(SpendHitDie2024(maxOf(1, it.total))) }, onDismiss = { rolling = null })
        }
    }
    if (addCond) ConditionPickerDialog(payload.activeConditions, onPick = { onApplyIntent(ApplyCondition2024(it)); addCond = false }, onDismiss = { addCond = false })
}

@Composable
private fun Spells2024(character: Character, payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit, onSave: (Character) -> Unit) {
    val primary = payload.classes.firstOrNull { CastingProgression.forClass(it.classId) != CastingProgression.NONE }
    var browse by remember { mutableStateOf<Boolean?>(null) } // null=closed; true=cantrip; false=leveled add
    var library by remember { mutableStateOf(false) }
    Page2024 {
        if (primary == null) {
            Card2024("Spellcasting") {
                Text("${payload.classes.firstOrNull()?.classId?.slugToTitle() ?: "This class"} isn't a spellcaster.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { library = true }) { Text("Browse Spell Library") }
            }
        } else {
            val classId = primary.classId
            Card2024("Spell Slots") {
                val max = payload.maxSpellSlots
                if (max.isEmpty()) Text("No spell slots at this level.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else max.keys.sorted().forEach { lvl ->
                    val rem = payload.currentSpellSlots[lvl] ?: 0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Level $lvl", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("●".repeat(rem) + "○".repeat((max.getValue(lvl) - rem).coerceAtLeast(0)), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                        TextButton(enabled = rem > 0, onClick = { onApplyIntent(ExpendSpellSlot2024(lvl)) }) { Text("Use") }
                    }
                }
                Text("Recover with a long rest on the Combat tab.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SpellListCard("Cantrips", payload.cantripsKnown, payload, onCast = { s -> onApplyIntent(CastSpell2024(s.id, s.name, 0, 0, requiresConcentration = s.concentration, applyToSelf = true)) }, onRemove = { s -> onSave(character.copy(payload = payload.copy(cantripsKnown = payload.cantripsKnown - s.id))) }, onAdd = { browse = true })
            SpellListCard("Prepared — ${classId.slugToTitle()}", payload.preparedSpells[classId].orEmpty(), payload, onCast = { s -> onApplyIntent(CastSpell2024(s.id, s.name, s.level, s.level, requiresConcentration = s.concentration, applyToSelf = true)) }, onRemove = { s -> onSave(character.copy(payload = payload.copy(preparedSpells = payload.preparedSpells + (classId to (payload.preparedSpells[classId].orEmpty() - s.id))))) }, onAdd = { browse = false })
            OutlinedButton(onClick = { library = true }, modifier = Modifier.fillMaxWidth()) { Text("Browse full Spell Library") }
        }
    }
    browse?.let { cantrips ->
        val have = if (cantrips) payload.cantripsKnown.toSet() else payload.preparedSpells.values.flatten().toSet()
        AddSpell2024Dialog(
            cantrips = cantrips,
            alreadyHave = have,
            onPick = { picked ->
                val cid = primary?.classId ?: "wizard"
                val updated = if (cantrips) payload.copy(cantripsKnown = (payload.cantripsKnown + picked.id).distinct())
                else payload.copy(preparedSpells = payload.preparedSpells + (cid to (payload.preparedSpells[cid].orEmpty() + picked.id).distinct()))
                onSave(character.copy(payload = updated)); browse = null
            },
            onDismiss = { browse = null },
        )
    }
    if (library) SpellLibrary2024Dialog { library = false }
}

@Composable
private fun Lore2024(character: Character, payload: DnD5e2024Payload) {
    Page2024 {
        Card2024("Identity") {
            StatRow("Name", character.name)
            StatRow("Species", payload.species.takeIf { it.isNotEmpty() }?.slugToTitle() ?: "—")
            StatRow("Class", payload.classes.takeIf { it.isNotEmpty() }?.joinToString(" / ") { "${it.classId.slugToTitle()} ${it.level}" } ?: "—")
            StatRow("Background", payload.background.takeIf { it.isNotEmpty() }?.slugToTitle() ?: "—")
            StatRow("Level", payload.level.toString())
            payload.alignment?.let { StatRow("Alignment", it) }
        }
        if (payload.originFeat != null || payload.chosenFeats.isNotEmpty() || payload.fightingStyle != null) {
            Card2024("Feats & Style") {
                payload.originFeat?.let { StatRow("Origin Feat", it.slugToTitle()) }
                payload.fightingStyle?.let { StatRow("Fighting Style", it.slugToTitle()) }
                payload.chosenFeats.forEach { StatRow("Feat", it.slugToTitle()) }
            }
        }
        if (payload.weaponMasteries.isNotEmpty()) {
            Card2024("Weapon Masteries") { FlowRowChips(payload.weaponMasteries.map { it.replaceFirstChar(Char::uppercase) }) {} }
        }
    }
}

// ── Shared atoms + dialogs ────────────────────────────────────────────────────

@Composable
private fun Page2024(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
}

@Composable
private fun Card2024(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Medallion2024(label: String, score: Int, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(AbilityScores.modifier(score).signed2024(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(score.toString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SpellListCard(title: String, ids: List<String>, payload: DnD5e2024Payload, onCast: (Spell2024) -> Unit, onRemove: (Spell2024) -> Unit, onAdd: () -> Unit) {
    val spells = ids.mapNotNull { DnD5e2024Catalog.spell(it) }.sortedWith(compareBy({ it.level }, { it.name }))
    Card2024(title) {
        if (spells.isEmpty()) Text("Nothing yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else spells.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            val hasSlot = s.level == 0 || (payload.currentSpellSlots[s.level] ?: 0) > 0
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, fontWeight = FontWeight.Medium)
                    Text("${s.levelLabel} · ${s.school}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(enabled = hasSlot, onClick = { onCast(s) }) { Text("Cast") }
                TextButton(onClick = { onRemove(s) }) { Text("✕") }
            }
        }
        OutlinedButton(onClick = onAdd) { Text("Add") }
    }
}

private enum class AmountKind(val label: String) { DAMAGE("Damage"), HEAL("Heal"), TEMP("Temp HP") }
private enum class RollKind { DEATH_SAVE, INITIATIVE, HIT_DIE }

@Composable
private fun AmountDialog(title: String, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableIntStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(enabled = amount > 1, onClick = { amount-- }) { Text("−", style = MaterialTheme.typography.headlineSmall) }
                Text("$amount", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { amount++ }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(amount) }) { Text(title) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddSpell2024Dialog(cantrips: Boolean, alreadyHave: Set<String>, onPick: (Spell2024) -> Unit, onDismiss: () -> Unit) {
    val pool = remember(cantrips) { DnD5e2024Catalog.spellLibrary.filter { if (cantrips) it.level == 0 else it.level >= 1 } }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) { pool.filter { it.id !in alreadyHave && (query.isBlank() || it.name.contains(query, ignoreCase = true)) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cantrips) "Add a cantrip" else "Add a spell") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("Search") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(filtered, key = { it.id }) { s ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(s) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Text(s.levelLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun SpellLibrary2024Dialog(onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) { DnD5e2024Catalog.spellLibrary.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) } }
    var expanded by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spell Library (SRD 5.2)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("Search ${DnD5e2024Catalog.spellLibrary.size} spells") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { it.id }) { s ->
                        Column(Modifier.fillMaxWidth().clickable { expanded = if (expanded == s.id) null else s.id }.padding(vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(s.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                Text(s.levelLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (expanded == s.id) {
                                Text("${s.school}${if (s.concentration) " · Concentration" else ""}${if (s.ritual) " · Ritual" else ""} · ${s.classNames.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(s.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionPickerDialog(active: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val standard = listOf("Blinded", "Charmed", "Deafened", "Frightened", "Grappled", "Incapacitated", "Invisible", "Paralyzed", "Petrified", "Poisoned", "Prone", "Restrained", "Stunned", "Unconscious")
    val activeLower = active.map { it.lowercase() }.toSet()
    var custom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add condition") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    standard.filter { it.lowercase() !in activeLower }.forEach { c -> AssistChip(onClick = { onPick(c) }, label = { Text(c) }) }
                }
                OutlinedTextField(custom, { custom = it }, label = { Text("Custom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (custom.isNotBlank()) TextButton(onClick = { onPick(custom.trim()) }) { Text("Add \"${custom.trim()}\"") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowChips(items: List<String>, onClick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { AssistChip(onClick = { onClick(it) }, label = { Text(it) }) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Effects2024Card(payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit) {
    if (payload.concentratingOn == null && payload.activeEffects.isEmpty()) return
    Card2024("Active Effects") {
        payload.concentratingOn?.let { focus ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Concentrating on $focus", Modifier.weight(1f), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { onApplyIntent(EndConcentration2024()) }) { Text("End") }
            }
        }
        if (payload.activeEffects.isEmpty()) Text("No lasting effects.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            payload.activeEffects.forEach { e ->
                AssistChip(
                    onClick = { onApplyIntent(CancelEffect2024(e.id)) },
                    label = { Text(e.name + e.modifiers.firstNotNullOfOrNull { it.shortLabel2024() }?.let { "  $it" }.orEmpty()) },
                    trailingIcon = { Text("✕", style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}

private fun EffectModifier.shortLabel2024(): String? = when (this) {
    is EffectModifier.AcBonus -> "${if (value >= 0) "+$value" else "$value"} AC"
    is EffectModifier.AcOverride -> "AC set"
    is EffectModifier.AttackBonus -> "${if (value >= 0) "+$value" else "$value"} atk"
    is EffectModifier.DamageBonus -> "${if (value >= 0) "+$value" else "$value"} dmg"
    is EffectModifier.SaveBonus -> "${if (value >= 0) "+$value" else "$value"} save"
    is EffectModifier.DamageResistance -> "resist $type"
    else -> null
}

private fun heroLine2024(payload: DnD5e2024Payload): String {
    val species = payload.species.takeIf { it.isNotEmpty() }?.slugToTitle()
    val cls = payload.classes.takeIf { it.isNotEmpty() }?.joinToString(" / ") { "${it.classId.slugToTitle()} ${it.level}" } ?: "Level ${payload.level}"
    return listOfNotNull(species, cls).joinToString(" · ")
}

private fun hitDie2024(payload: DnD5e2024Payload): Int =
    au.com.evonet.nat20.dnd5e.core.DnD5eClasses.hitDie(payload.classes.firstOrNull()?.classId ?: "")

private fun Int.signed2024(): String = if (this >= 0) "+$this" else "$this"
