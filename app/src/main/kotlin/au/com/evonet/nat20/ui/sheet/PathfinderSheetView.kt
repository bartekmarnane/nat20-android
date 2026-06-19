package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.pf2e.PathfinderConditions
import au.com.evonet.nat20.pf2e.PathfinderPayload
import au.com.evonet.nat20.pf2e.PfAddNote
import au.com.evonet.nat20.pf2e.PfAddWeapon
import au.com.evonet.nat20.pf2e.PfAdjustHeroPoints
import au.com.evonet.nat20.pf2e.PfApplyCondition
import au.com.evonet.nat20.pf2e.PfArmors
import au.com.evonet.nat20.pf2e.PfCastFocusSpell
import au.com.evonet.nat20.pf2e.PfCastSpell
import au.com.evonet.nat20.pf2e.Bulk
import au.com.evonet.nat20.pf2e.ClassProgression
import au.com.evonet.nat20.pf2e.FeatSlots
import au.com.evonet.nat20.pf2e.PFCoin
import au.com.evonet.nat20.pf2e.PFInventoryItem
import au.com.evonet.nat20.pf2e.PfAddInventoryItem
import au.com.evonet.nat20.pf2e.PfAdjustCoin
import au.com.evonet.nat20.pf2e.PfRemoveInventoryItem
import au.com.evonet.nat20.pf2e.PfSetArmorRunes
import au.com.evonet.nat20.pf2e.PfSetWeaponRunes
import au.com.evonet.nat20.pf2e.Runes
import au.com.evonet.nat20.pf2e.Wealth
import au.com.evonet.nat20.pf2e.encumberedThreshold
import au.com.evonet.nat20.pf2e.isEncumbered
import au.com.evonet.nat20.pf2e.totalBulk
import au.com.evonet.nat20.pf2e.PfClearCondition
import au.com.evonet.nat20.pf2e.PfDailyPreparations
import au.com.evonet.nat20.pf2e.PfFeatType
import au.com.evonet.nat20.pf2e.PfFeats
import au.com.evonet.nat20.pf2e.PfRemoveFeat
import au.com.evonet.nat20.pf2e.PfTakeFeat
import au.com.evonet.nat20.pf2e.PfLearnSpell
import au.com.evonet.nat20.pf2e.PfRefocus
import au.com.evonet.nat20.pf2e.PfSpells
import au.com.evonet.nat20.pf2e.PfEquipArmor
import au.com.evonet.nat20.pf2e.PfEquipShield
import au.com.evonet.nat20.pf2e.PfGainTempHp
import au.com.evonet.nat20.pf2e.PfHeal
import au.com.evonet.nat20.pf2e.PfLevelUp
import au.com.evonet.nat20.pf2e.PfRaiseShield
import au.com.evonet.nat20.pf2e.PfRemoveWeapon
import au.com.evonet.nat20.pf2e.PfSetDying
import au.com.evonet.nat20.pf2e.PfShields
import au.com.evonet.nat20.pf2e.PfTakeDamage
import au.com.evonet.nat20.pf2e.PfWeapons
import au.com.evonet.nat20.pf2e.armorClass
import au.com.evonet.nat20.pf2e.armorClassRaised
import au.com.evonet.nat20.pf2e.classDcValue
import au.com.evonet.nat20.pf2e.isCaster
import au.com.evonet.nat20.pf2e.maxSpellSlots
import au.com.evonet.nat20.pf2e.spellAttack
import au.com.evonet.nat20.pf2e.spellDc
import au.com.evonet.nat20.pf2e.strikes
import au.com.evonet.nat20.pf2e.core.AdvancementSchedule
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save
import au.com.evonet.nat20.pf2e.modifier
import au.com.evonet.nat20.pf2e.perceptionBonus
import au.com.evonet.nat20.pf2e.saveBonus
import au.com.evonet.nat20.pf2e.skillBonus
import au.com.evonet.nat20.ui.slugToTitle
import kotlinx.coroutines.launch

/**
 * The Pathfinder 2e (Remaster) character sheet — the foundation slice (A22).
 * Interactive Stats / Skills / Combat / Lore tabs typed to [PathfinderPayload],
 * surfacing what makes PF2e PF2e: per-statistic proficiency ranks (U/T/E/M/L),
 * the dying/wounded track, Hero Points, and valued conditions. Spells, equipment,
 * feats, and the action economy are follow-up slices.
 */
private enum class PfTab(val title: String) { STATS("Stats"), SKILLS("Skills"), COMBAT("Combat"), SPELLS("Spells"), FEATS("Feats"), LORE("Lore") }

@Composable
fun PathfinderSheetView(character: Character, onApplyIntent: (CharacterIntent) -> Unit, modifier: Modifier = Modifier) {
    val payload = character.payload as? PathfinderPayload ?: return
    val tabs = PfTab.entries
    val pager = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(character.name, style = MaterialTheme.typography.headlineSmall)
            Text(pfHeroLine(payload), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Pathfinder 2e (Remaster)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            when (tabs[page]) {
                PfTab.STATS -> PfStats(payload, onApplyIntent)
                PfTab.SKILLS -> PfSkills(payload)
                PfTab.COMBAT -> PfCombat(payload, onApplyIntent)
                PfTab.SPELLS -> PfSpellsTab(payload, onApplyIntent)
                PfTab.FEATS -> PfFeatsTab(payload, onApplyIntent)
                PfTab.LORE -> PfLore(character, payload, onApplyIntent)
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
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PfStats(payload: PathfinderPayload, onApplyIntent: (CharacterIntent) -> Unit) {
    var levelingUp by remember { mutableStateOf(false) }
    PfPage {
        PfCard("Abilities") {
            PfAbility.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { a ->
                        PfMedallion(a.abbreviation, payload.abilityScores.score(a), Modifier.weight(1f))
                    }
                }
            }
        }
        PfCard("Defenses & Perception") {
            PfStatRow("Armor Class", payload.armorClass.toString())
            PfProfRow("Perception", payload.perceptionBonus, payload.perception)
            Save.entries.forEach { s -> PfProfRow(s.displayName, payload.saveBonus(s), payload.saves[s] ?: Proficiency.UNTRAINED) }
            PfStatRow("Class DC", payload.classDcValue.toString())
        }
        if (payload.level < PathfinderPayload.MAX_LEVEL) {
            androidx.compose.material3.Button(onClick = { levelingUp = true }, modifier = Modifier.fillMaxWidth()) { Text("Level Up") }
        }
    }
    if (levelingUp) PfLevelUpDialog(payload, onApply = { onApplyIntent(it); levelingUp = false }, onDismiss = { levelingUp = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PfLevelUpDialog(payload: PathfinderPayload, onApply: (PfLevelUp) -> Unit, onDismiss: () -> Unit) {
    val newLevel = payload.level + 1
    val grantsSkill = AdvancementSchedule.grantsSkillIncrease(newLevel)
    val grantsBoosts = AdvancementSchedule.grantsAbilityBoosts(newLevel)
    val maxRank = AdvancementSchedule.maxSkillRank(newLevel)
    var skill by remember { mutableStateOf<PfSkill?>(null) }
    var boosts by remember { mutableStateOf<List<PfAbility>>(emptyList()) }
    val ready = (!grantsBoosts || boosts.size == 4)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Level Up → $newLevel") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("HP and every proficiency-scaled statistic improve automatically.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val jumps = ClassProgression.increasesAt(payload.className, newLevel)
                if (jumps.isNotEmpty()) {
                    Text("Class advances: " + jumps.joinToString(", ") { "${it.track.name.lowercase().replaceFirstChar(Char::uppercase)} → ${it.rank.displayName}" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (grantsSkill) {
                    PfSectionLabel("Skill increase (to ≤ ${maxRank.displayName})")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PfSkill.entries.forEach { s ->
                            val cur = payload.skills[s] ?: Proficiency.UNTRAINED
                            val canRaise = (cur.next?.rank ?: 99) <= maxRank.rank
                            FilterChip(skill == s, enabled = canRaise, onClick = { skill = if (skill == s) null else s }, label = { Text("${s.displayName} ${cur.letter}→${cur.next?.letter ?: "—"}") })
                        }
                    }
                }
                if (grantsBoosts) {
                    PfSectionLabel("Ability boosts (${boosts.size}/4)")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PfAbility.entries.forEach { a ->
                            val on = a in boosts
                            FilterChip(on, enabled = on || boosts.size < 4, onClick = { boosts = if (on) boosts - a else boosts + a }, label = { Text("${a.abbreviation} ${payload.abilityScores.score(a)}") })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = ready, onClick = { onApply(PfLevelUp(skill, boosts)) }) { Text("Level Up") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PfSkills(payload: PathfinderPayload) {
    PfPage {
        PfCard("Skills") {
            PfSkill.entries.forEachIndexed { i, skill ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                val rank = payload.skills[skill] ?: Proficiency.UNTRAINED
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(rank.letter, color = if (rank == Proficiency.UNTRAINED) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(skill.displayName, Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                    Text(skill.ability.abbreviation, Modifier.padding(end = 12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(payload.skillBonus(skill).signedPf(), fontWeight = FontWeight.Bold)
                }
            }
        }
        if (payload.loreSkills.isNotEmpty()) {
            PfCard("Lore") {
                payload.loreSkills.forEach { (subtype, rank) ->
                    PfProfRow(subtype, payload.modifier(PfAbility.INTELLIGENCE) + rank.bonus(payload.level), rank)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PfCombat(payload: PathfinderPayload, onApplyIntent: (CharacterIntent) -> Unit) {
    var amount by remember { mutableStateOf<PfAmount?>(null) }
    var addCond by remember { mutableStateOf(false) }
    var pickArmor by remember { mutableStateOf(false) }
    var pickShield by remember { mutableStateOf(false) }
    var addWeapon by remember { mutableStateOf(false) }
    var armorRunes by remember { mutableStateOf(false) }
    var weaponRunesFor by remember { mutableStateOf<String?>(null) }
    var addItem by remember { mutableStateOf(false) }
    var adjustCoin by remember { mutableStateOf<PFCoin?>(null) }
    PfPage {
        PfCard("Hit Points") {
            PfStatRow("Current / Max", "${payload.currentHp} / ${payload.maxHp}" + if (payload.temporaryHp > 0) " (+${payload.temporaryHp})" else "")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { amount = PfAmount.DAMAGE }) { Text("Damage") }
                OutlinedButton(onClick = { amount = PfAmount.HEAL }) { Text("Heal") }
                OutlinedButton(onClick = { amount = PfAmount.TEMP }) { Text("Temp HP") }
            }
        }
        if (payload.dying > 0 || payload.wounded > 0 || payload.currentHp == 0) {
            PfCard("Dying & Wounded") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Dying ${payload.dying} / ${PathfinderPayload.DYING_MAX}", style = MaterialTheme.typography.bodyMedium, color = if (payload.isDying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        if (payload.wounded > 0) Text("Wounded ${payload.wounded}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(enabled = payload.dying > 0, onClick = { onApplyIntent(PfSetDying(payload.dying - 1)) }) { Text("Recover") }
                    TextButton(enabled = payload.dying < PathfinderPayload.DYING_MAX, onClick = { onApplyIntent(PfSetDying(payload.dying + 1)) }) { Text("+1") }
                }
            }
        }
        PfCard("Hero Points") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("●".repeat(payload.heroPoints) + "○".repeat((PathfinderPayload.HERO_POINT_MAX - payload.heroPoints).coerceAtLeast(0)), Modifier.weight(1f), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                TextButton(enabled = payload.heroPoints > 0, onClick = { onApplyIntent(PfAdjustHeroPoints(-1)) }) { Text("Spend") }
                TextButton(enabled = payload.heroPoints < PathfinderPayload.HERO_POINT_MAX, onClick = { onApplyIntent(PfAdjustHeroPoints(1)) }) { Text("Gain") }
            }
        }
        PfCard("Armor & Shield") {
            val worn = payload.armor?.let { PfArmors.by(it) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(worn?.let { "${it.name} (AC +${it.acBonus}, Dex cap ${it.dexCap})" } ?: "Unarmored", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { pickArmor = true }) { Text("Change") }
            }
            val shield = payload.shield?.let { PfShields.by(it) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(shield?.let { "${it.name} (+${it.raisedAcBonus} raised)" + if (payload.shieldRaised) " · Raised → AC ${payload.armorClassRaised}" else "" } ?: "No shield", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = if (payload.shieldRaised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                if (shield != null) OutlinedButton(onClick = { onApplyIntent(PfRaiseShield(!payload.shieldRaised)) }) { Text(if (payload.shieldRaised) "Lower" else "Raise") }
                OutlinedButton(onClick = { pickShield = true }) { Text(if (shield == null) "Hold" else "Stow") }
            }
            if (worn != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (payload.armorRunes.isEmpty) "No runes" else "Runes: +${payload.armorRunes.potency}" + if (payload.armorRunes.resilient > 0) " · resilient ${payload.armorRunes.resilient}" else "", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { armorRunes = true }) { Text("Etch") }
            }
        }
        PfCard("Strikes") {
            if (payload.strikes.isEmpty()) Text("No weapons.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else payload.strikes.forEachIndexed { i, s ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val r = payload.weaponRunes[s.weapon.id]
                        Text(s.weapon.name + (r?.takeIf { !it.isEmpty }?.let { " (+${it.potency}${if (it.striking > 0) " striking" else ""})" } ?: ""), fontWeight = FontWeight.Medium)
                        Text("Atk " + s.attackMods.joinToString(" / ") { it.signedPf() } + "  ·  ${s.damage} ${s.damageType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { weaponRunesFor = s.weapon.id }) { Text("Etch") }
                    TextButton(onClick = { onApplyIntent(PfRemoveWeapon(s.weapon.id)) }) { Text("✕") }
                }
            }
            OutlinedButton(onClick = { addWeapon = true }) { Text("Add weapon") }
        }
        PfCard("Inventory & Coins") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Purse: ${Wealth.purseLabel(payload.coins)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                PFCoin.entries.reversed().forEach { c -> TextButton(onClick = { adjustCoin = c }) { Text(c.abbreviation) } }
            }
            Text("Bulk ${Bulk.effective(payload.totalBulk)} / ${payload.encumberedThreshold}" + if (payload.isEncumbered) " · Encumbered" else "", style = MaterialTheme.typography.labelSmall, color = if (payload.isEncumbered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            if (payload.inventory.isEmpty()) Text("Pack is empty.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else payload.inventory.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text((if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name) + " · Bulk ${Bulk.label(item.bulk)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { onApplyIntent(PfRemoveInventoryItem(item.name)) }) { Text("✕") }
                }
            }
            OutlinedButton(onClick = { addItem = true }) { Text("Add item") }
        }
        PfCard("Conditions") {
            if (payload.conditions.isEmpty()) Text("No conditions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                payload.conditions.forEach { c ->
                    AssistChip(onClick = { onApplyIntent(PfClearCondition(c.id)) }, label = { Text(c.label(PathfinderConditions.displayName(c.id))) }, trailingIcon = { Text("✕", style = MaterialTheme.typography.labelMedium) })
                }
            }
            OutlinedButton(onClick = { addCond = true }) { Text("Add condition") }
        }
    }
    if (pickArmor) PfChoiceDialog("Choose armor", listOf(null to "Unarmored") + PfArmors.all.map { it.id to "${it.name} · ${it.category.displayName} +${it.acBonus}" }, onPick = { onApplyIntent(PfEquipArmor(it)); pickArmor = false }, onDismiss = { pickArmor = false })
    if (pickShield) PfChoiceDialog("Choose shield", listOf(null to "None") + PfShields.all.map { it.id to "${it.name} (+${it.raisedAcBonus})" }, onPick = { onApplyIntent(PfEquipShield(it)); pickShield = false }, onDismiss = { pickShield = false })
    if (addWeapon) PfChoiceDialog("Add weapon", PfWeapons.all.map { it.id as String? to "${it.name} · ${it.category.displayName} ${it.damageDie}${it.damageType}" }, onPick = { it?.let { id -> onApplyIntent(PfAddWeapon(id)) }; addWeapon = false }, onDismiss = { addWeapon = false })
    if (armorRunes) PfRuneDialog("Armor runes", payload.armorRunes.potency, payload.armorRunes.resilient, "Resilient", onApply = { pot, sec -> onApplyIntent(PfSetArmorRunes(pot, sec)); armorRunes = false }, onDismiss = { armorRunes = false })
    weaponRunesFor?.let { wid ->
        val r = payload.weaponRunes[wid]
        PfRuneDialog("Weapon runes", r?.potency ?: 0, r?.striking ?: 0, "Striking", onApply = { pot, sec -> onApplyIntent(PfSetWeaponRunes(wid, pot, sec)); weaponRunesFor = null }, onDismiss = { weaponRunesFor = null })
    }
    if (addItem) PfItemDialog(onAdd = { onApplyIntent(PfAddInventoryItem(it)); addItem = false }, onDismiss = { addItem = false })
    adjustCoin?.let { coin -> PfCoinDialog(coin, payload.coins[coin] ?: 0, onConfirm = { d -> if (d != 0) onApplyIntent(PfAdjustCoin(coin, d)); adjustCoin = null }, onDismiss = { adjustCoin = null }) }
    amount?.let { kind ->
        PfAmountDialog(kind.label, onConfirm = { n ->
            onApplyIntent(when (kind) { PfAmount.DAMAGE -> PfTakeDamage(n); PfAmount.HEAL -> PfHeal(n); PfAmount.TEMP -> PfGainTempHp(n) })
            amount = null
        }, onDismiss = { amount = null })
    }
    if (addCond) PfConditionPicker(payload.conditions.map { it.id }.toSet(), onPick = { id, value -> onApplyIntent(PfApplyCondition(id, value)); addCond = false }, onDismiss = { addCond = false })
}

@Composable
private fun PfSpellsTab(payload: PathfinderPayload, onApplyIntent: (CharacterIntent) -> Unit) {
    var browse by remember { mutableStateOf<Int?>(null) } // rank to add (0 = cantrip); null = closed
    PfPage {
        if (!payload.isCaster) {
            PfCard("Spellcasting") { Text("${payload.className.takeIf { it.isNotEmpty() }?.slugToTitle() ?: "This class"} isn't a spellcaster.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            PfCard("${payload.spellTradition?.displayName} Spellcasting") {
                PfStatRow("Spell Attack", payload.spellAttack.signedPf())
                PfStatRow("Spell DC", payload.spellDc.toString())
            }
            PfCard("Spell Slots") {
                val max = payload.maxSpellSlots
                if (max.isEmpty()) Text("No spell slots yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else max.keys.sorted().forEach { rank ->
                    val rem = payload.currentSpellSlots[rank] ?: 0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Rank $rank", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("●".repeat(rem) + "○".repeat((max.getValue(rank) - rem).coerceAtLeast(0)), color = MaterialTheme.colorScheme.primary)
                    }
                }
                OutlinedButton(onClick = { onApplyIntent(PfDailyPreparations()) }) { Text("Daily preparations") }
            }
            if (payload.maxFocusPoints > 0) {
                PfCard("Focus Pool") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("●".repeat(payload.focusPoints) + "○".repeat((payload.maxFocusPoints - payload.focusPoints).coerceAtLeast(0)), Modifier.weight(1f), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        payload.focusSpells.mapNotNull { PfSpells.by(it) }.forEach { s ->
                            TextButton(enabled = payload.focusPoints > 0, onClick = { onApplyIntent(PfCastFocusSpell(s.name)) }) { Text(s.name) }
                        }
                        OutlinedButton(onClick = { onApplyIntent(PfRefocus()) }) { Text("Refocus") }
                    }
                }
            }
            SpellListCard("Cantrips", payload.cantrips, payload, onCast = { onApplyIntent(PfCastSpell(it.id, it.name, 0, 0)) }, onAdd = { browse = 0 })
            payload.knownSpells.keys.sorted().forEach { rank ->
                SpellListCard("Rank $rank", payload.knownSpells[rank].orEmpty(), payload, onCast = { onApplyIntent(PfCastSpell(it.id, it.name, it.rank, rank)) }, onAdd = { browse = rank })
            }
            OutlinedButton(onClick = { browse = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Add a spell") }
        }
    }
    browse?.let { rank ->
        val tradition = payload.spellTradition
        val pool = PfSpells.all.filter { (tradition == null || tradition in it.traditions) && (if (rank == 0) it.rank == 0 else it.rank in 1..rank) }
        PfChoiceDialog("Add a spell", pool.map { it.id as String? to "${it.name} · ${it.rankLabel}" }, onPick = { id -> id?.let { onApplyIntent(PfLearnSpell(it, if (rank == 0) 0 else PfSpells.by(it)!!.rank)) }; browse = null }, onDismiss = { browse = null })
    }
}

@Composable
private fun SpellListCard(title: String, ids: List<String>, payload: PathfinderPayload, onCast: (au.com.evonet.nat20.pf2e.PfSpell) -> Unit, onAdd: () -> Unit) {
    val spells = ids.mapNotNull { PfSpells.by(it) }.sortedWith(compareBy({ it.rank }, { it.name }))
    PfCard(title) {
        if (spells.isEmpty()) Text("Nothing yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else spells.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, fontWeight = FontWeight.Medium)
                    Text("${s.actions} · ${s.traits.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onCast(s) }) { Text("Cast") }
            }
        }
    }
}

@Composable
private fun PfFeatsTab(payload: PathfinderPayload, onApplyIntent: (CharacterIntent) -> Unit) {
    var adding by remember { mutableStateOf<PfFeatType?>(null) }
    val taken = payload.feats.mapNotNull { PfFeats.by(it) }
    val isRogue = payload.className.equals("rogue", ignoreCase = true)
    PfPage {
        PfFeatType.entries.forEach { type ->
            val have = taken.filter { it.type == type }
            val slots = when (type) {
                PfFeatType.ANCESTRY -> FeatSlots.ancestry(payload.level)
                PfFeatType.CLASS -> FeatSlots.classFeat(payload.level, payload.className)
                PfFeatType.SKILL -> FeatSlots.skill(payload.level, isRogue)
                PfFeatType.GENERAL -> FeatSlots.general(payload.level)
            }
            PfCard("${type.displayName} Feats (${have.size}/$slots)") {
                if (have.isEmpty()) Text("None taken.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else have.forEachIndexed { i, f ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${f.name} · Lv ${f.level}", fontWeight = FontWeight.Medium)
                            Text(f.summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onApplyIntent(PfRemoveFeat(f.id)) }) { Text("✕") }
                    }
                }
                OutlinedButton(onClick = { adding = type }) { Text("Add ${type.displayName.lowercase()} feat") }
            }
        }
    }
    adding?.let { type ->
        val pool = PfFeats.available(type, payload.ancestry, payload.className, payload.level).filter { it.id !in payload.feats }
        PfChoiceDialog("Add ${type.displayName} feat", pool.map { it.id as String? to "${it.name} · Lv ${it.level} — ${it.summary}" }, onPick = { id -> id?.let { onApplyIntent(PfTakeFeat(it)) }; adding = null }, onDismiss = { adding = null })
    }
}

@Composable
private fun PfLore(character: Character, payload: PathfinderPayload, onApplyIntent: (CharacterIntent) -> Unit) {
    var note by remember { mutableStateOf<Boolean>(false) }
    PfPage {
        PfCard("Identity") {
            PfStatRow("Name", character.name)
            PfStatRow("Ancestry", listOfNotNull(payload.heritage?.slugToTitle(), payload.ancestry.takeIf { it.isNotEmpty() }?.slugToTitle()).joinToString(" ").ifEmpty { "—" })
            PfStatRow("Background", payload.background?.slugToTitle() ?: "—")
            PfStatRow("Class", payload.className.takeIf { it.isNotEmpty() }?.slugToTitle()?.let { "$it ${payload.level}" } ?: "Level ${payload.level}")
            payload.keyAbility?.let { PfStatRow("Key Ability", it.displayName) }
            payload.alignmentOrEdicts?.let { PfStatRow("Edicts / Anathema", it) }
        }
        OutlinedButton(onClick = { note = true }) { Text("Add a note") }
    }
    if (note) PfNoteDialog(onConfirm = { onApplyIntent(PfAddNote(it)); note = false }, onDismiss = { note = false })
}

// ── atoms ──────────────────────────────────────────────────────────────────────

@Composable
private fun PfPage(content: @Composable () -> Unit) =
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }

@Composable
private fun PfCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun PfSectionLabel(text: String) = Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

@Composable
private fun PfStatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PfProfRow(label: String, bonus: Int, rank: Proficiency) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(rank.letter, color = if (rank == Proficiency.UNTRAINED) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(label, Modifier.weight(1f).padding(start = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(bonus.signedPf(), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PfMedallion(label: String, score: Int, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(PfAbilityScores.modifier(score).signedPf(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(score.toString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private enum class PfAmount(val label: String) { DAMAGE("Damage"), HEAL("Heal"), TEMP("Temp HP") }

@Composable
private fun PfAmountDialog(title: String, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var n by remember { mutableIntStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(enabled = n > 1, onClick = { n-- }) { Text("−", style = MaterialTheme.typography.headlineSmall) }
                Text("$n", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { n++ }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(n) }) { Text(title) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PfConditionPicker(active: Set<String>, onPick: (String, Int?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add condition") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PathfinderConditions.all.filter { it.id !in active }.forEach { c ->
                    AssistChip(onClick = { onPick(c.id, if (c.valued) 1 else null) }, label = { Text(c.displayName + if (c.valued) " 1" else "") })
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun PfChoiceDialog(title: String, options: List<Pair<String?, String>>, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (id, label) ->
                    Text(label, Modifier.fillMaxWidth().clickable { onPick(id) }.padding(vertical = 10.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun PfNoteDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a note") },
        text = { OutlinedTextField(text, { text = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PfRuneDialog(title: String, potency: Int, secondary: Int, secondaryLabel: String, onApply: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    var pot by remember { mutableIntStateOf(potency) }
    var sec by remember { mutableIntStateOf(secondary) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PfStepperRow("Potency", pot, 0..Runes.MAX_TIER) { pot = it }
                PfStepperRow(secondaryLabel, sec, 0..Runes.MAX_TIER) { sec = it }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(pot, sec) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PfStepperRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        TextButton(enabled = value > range.first, onClick = { onChange(value - 1) }) { Text("−", style = MaterialTheme.typography.titleMedium) }
        Text("+$value", fontWeight = FontWeight.Bold)
        TextButton(enabled = value < range.last, onClick = { onChange(value + 1) }) { Text("+", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun PfCoinDialog(coin: PFCoin, current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var delta by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust ${coin.abbreviation}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Have $current ${coin.abbreviation}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(enabled = current + delta > 0, onClick = { delta-- }) { Text("−", style = MaterialTheme.typography.headlineSmall) }
                    Text(if (delta >= 0) "+$delta" else "$delta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { delta++ }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(delta) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PfItemDialog(onAdd: (PFInventoryItem) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var light by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Item name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bulk", Modifier.weight(1f))
                    FilterChip(!light, { light = false }, label = { Text("—") })
                    FilterChip(light, { light = true }, label = { Text("L") })
                }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onAdd(PFInventoryItem(name.trim(), 1, if (light) Bulk.LIGHT else 0.0)) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun pfHeroLine(payload: PathfinderPayload): String {
    val ancestry = payload.ancestry.takeIf { it.isNotEmpty() }?.slugToTitle()
    val cls = payload.className.takeIf { it.isNotEmpty() }?.slugToTitle()?.let { "$it ${payload.level}" } ?: "Level ${payload.level}"
    return listOfNotNull(ancestry, cls).joinToString(" · ")
}

private fun Int.signedPf(): String = if (this >= 0) "+$this" else "$this"
