package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.CastingProgression
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e2024.AcquireItem2024
import au.com.evonet.nat20.dnd5e2024.AdjustCoin2024
import au.com.evonet.nat20.dnd5e2024.ApplyCondition2024
import au.com.evonet.nat20.dnd5e2024.ArmorClass2024
import au.com.evonet.nat20.dnd5e2024.Armors2024
import au.com.evonet.nat20.dnd5e2024.AttackMath2024
import au.com.evonet.nat20.dnd5e2024.CancelEffect2024
import au.com.evonet.nat20.dnd5e2024.CastSpell2024
import au.com.evonet.nat20.dnd5e2024.ChangeExhaustion2024
import au.com.evonet.nat20.dnd5e2024.ClearCondition2024
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Catalog
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.dnd5e2024.DropItem2024
import au.com.evonet.nat20.dnd5e2024.EndConcentration2024
import au.com.evonet.nat20.dnd5e2024.EquipArmor2024
import au.com.evonet.nat20.dnd5e2024.Exhaustion2024
import au.com.evonet.nat20.dnd5e2024.ExpendSpellSlot2024
import au.com.evonet.nat20.dnd5e2024.Feats2024
import au.com.evonet.nat20.dnd5e2024.GainTempHp2024
import au.com.evonet.nat20.dnd5e2024.Heal2024
import au.com.evonet.nat20.dnd5e2024.InventoryItem2024
import au.com.evonet.nat20.dnd5e2024.ItemKind2024
import au.com.evonet.nat20.dnd5e2024.LongRest2024
import au.com.evonet.nat20.dnd5e2024.PrepareSpell2024
import au.com.evonet.nat20.dnd5e2024.RollDeathSave2024
import au.com.evonet.nat20.dnd5e2024.SetInitiative2024
import au.com.evonet.nat20.dnd5e2024.SetInspiration2024
import au.com.evonet.nat20.dnd5e2024.SetShield2024
import au.com.evonet.nat20.dnd5e2024.SetWeaponMasteries2024
import au.com.evonet.nat20.dnd5e2024.SpeciesTraits2024
import au.com.evonet.nat20.dnd5e2024.Spell2024
import au.com.evonet.nat20.dnd5e2024.SpendHitDie2024
import au.com.evonet.nat20.dnd5e2024.TakeDamage2024
import au.com.evonet.nat20.dnd5e2024.UnprepareSpell2024
import au.com.evonet.nat20.dnd5e2024.WeaponMastery2024
import au.com.evonet.nat20.dnd5e2024.WeaponMasteryProgression2024
import au.com.evonet.nat20.dnd5e2024.Weapons2024
import au.com.evonet.nat20.dnd5e2024.armorClass
import au.com.evonet.nat20.dnd5e2024.effectiveDamageResistances
import au.com.evonet.nat20.dnd5e2024.effectiveMaxHp
import au.com.evonet.nat20.dnd5e2024.effectiveSkillProficiencies
import au.com.evonet.nat20.dnd5e2024.initiativeBonus
import au.com.evonet.nat20.dnd5e2024.temporarySaveBonus
import au.com.evonet.nat20.dnd5e2024.temporarySkillBonus
import au.com.evonet.nat20.dnd5e2024.weapons
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.ui.actions.DnD5e2024ActionsLayer
import au.com.evonet.nat20.ui.codex.AbilityMedallion
import au.com.evonet.nat20.ui.codex.CodexButton
import au.com.evonet.nat20.ui.codex.CodexCapsuleButton
import au.com.evonet.nat20.ui.codex.CodexEffectChip
import au.com.evonet.nat20.ui.codex.CodexPage
import au.com.evonet.nat20.ui.codex.CodexScaffold
import au.com.evonet.nat20.ui.codex.CoinTile
import au.com.evonet.nat20.ui.codex.DashedNotice
import au.com.evonet.nat20.ui.codex.DashedRule
import au.com.evonet.nat20.ui.codex.FeatureBlock
import au.com.evonet.nat20.ui.codex.HPBlock
import au.com.evonet.nat20.ui.codex.HairlineRule
import au.com.evonet.nat20.ui.codex.InspirationBanner
import au.com.evonet.nat20.ui.codex.SaveCell
import au.com.evonet.nat20.ui.codex.SectionHead
import au.com.evonet.nat20.ui.codex.SkillPip
import au.com.evonet.nat20.ui.codex.SlotTile
import au.com.evonet.nat20.ui.codex.StatChip
import au.com.evonet.nat20.ui.codex.signed
import au.com.evonet.nat20.ui.identity.IdentitySheet
import au.com.evonet.nat20.ui.roll.RollDialog
import au.com.evonet.nat20.ui.slugToTitle
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.Diamond
import au.com.evonet.nat20.ui.theme.DiamondShape
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette
import java.time.Instant

/**
 * The interactive D&D 5e (2024) codex (A21 / parity #24–30): the shared parchment
 * [CodexScaffold] (nav row, hero, campaign region, tab bar) wrapped around six
 * `CodexAtoms`-styled tab bodies. It reuses the shared `-core` machinery, the
 * `:app` die-roll primitive, and the 2014 skill list (skills are edition-agnostic),
 * and journals through the same `onApplyIntent`.
 *
 * Deltas from the 2014 codex: a species hero line, a heroic-inspiration banner, the
 * cumulative 2024 exhaustion line, the weapon-mastery section, an all-casters-prepare
 * Spells tab (via [PrepareSpell2024]/[UnprepareSpell2024]), and `kind.displayName`
 * item rows over the 2024 armor/shield/mastery gear editors. In-campaign, the Act
 * pill opens the grouped [DnD5e2024ActionsLayer] (parity #31); the Combat tab's
 * direct vitals/rest/condition editors are kept as the building-phase edit path
 * (no Act pill while building).
 */
private enum class Tab2024(val title: String) {
    STATS("Stats"), SKILLS("Skills"), COMBAT("Combat"),
    SPELLS("Spells"), ITEMS("Items"), LORE("Lore"),
}

@Composable
fun Codex2024ShellView(
    character: Character,
    activeCampaign: Campaign?,
    hasPastAdventures: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStartCampaign: () -> Unit,
    onEndCampaign: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPastAdventures: () -> Unit,
    onApplyIntent: (CharacterIntent) -> Unit,
    onSave: (Character) -> Unit,
    modifier: Modifier = Modifier,
) {
    val payload = character.payload as? DnD5e2024Payload ?: return
    val inCampaign = character.phase is CharacterPhase.InCampaign
    val tabs = Tab2024.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    var levelingUp by remember { mutableStateOf(false) }
    var editingIdentity by remember { mutableStateOf(false) }
    // Act pill (in-campaign) opens the grouped 2024 actions layer (parity #31).
    var showActions by remember { mutableStateOf(false) }

    CodexScaffold(
        pagerState = pagerState,
        tabLabels = tabs.map { it.title },
        heroName = character.name,
        heroSubtitleLines = listOf(speciesLine2024(payload), classAndLevelLine2024(payload)),
        portraitData = character.portraitData,
        fallbackLetter = character.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        editableHero = inCampaign,
        onEditIdentity = { editingIdentity = true },
        inCampaign = inCampaign,
        campaignName = activeCampaign?.name,
        hasPastAdventures = hasPastAdventures,
        onBack = onBack,
        onEdit = onEdit,
        onAct = { showActions = true },
        onStartCampaign = onStartCampaign,
        onEndCampaign = onEndCampaign,
        onOpenJournal = onOpenJournal,
        onOpenPastAdventures = onOpenPastAdventures,
        modifier = modifier,
    ) { page ->
        when (tabs[page]) {
            Tab2024.STATS -> Stats2024(payload, onApplyIntent) { levelingUp = true }
            Tab2024.SKILLS -> Skills2024(payload)
            Tab2024.COMBAT -> Combat2024(payload, onApplyIntent)
            Tab2024.SPELLS -> Spells2024(character, payload, onApplyIntent, onSave)
            Tab2024.ITEMS -> Items2024(payload, onApplyIntent)
            Tab2024.LORE -> Lore2024(character, payload)
        }
    }

    if (levelingUp) {
        LevelUp2024Wizard(payload, onApplyIntent, onDismiss = { levelingUp = false })
    }

    DnD5e2024ActionsLayer(
        payload = payload,
        showSheet = showActions && inCampaign,
        onDismissSheet = { showActions = false },
        onApplyIntent = onApplyIntent,
    )

    if (editingIdentity) {
        IdentitySheet(
            initialName = character.name,
            initialPortrait = character.portraitData,
            onCancel = { editingIdentity = false },
            onCommit = { newName, portrait ->
                onSave(character.copy(name = newName, portraitData = portrait, updatedAt = Instant.now()))
                editingIdentity = false
            },
        )
    }
}

// ── Stats ─────────────────────────────────────────────────────────────────────

@Composable
private fun Stats2024(payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit, onLevelUp: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val scores = payload.effectiveAbilityScores
    val prof = Proficiency.bonus(payload.level)
    val proficientSaves = payload.classes.firstOrNull()?.classId
        ?.let { DnD5e2024Catalog.classes.firstOrNull { c -> c.id == it }?.savingThrows }.orEmpty().toSet()
    var check by remember { mutableStateOf<CheckRoll2024?>(null) }
    var acBreakdown by remember { mutableStateOf(false) }

    CodexPage {
        // Chip row: Init / AC / Prof / Speed.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Init", payload.initiative?.toString() ?: payload.initiativeBonus.signed(), Modifier.weight(1f))
            StatChip("AC", payload.armorClass.toString(), Modifier.weight(1f), onClick = { acBreakdown = true })
            StatChip("Prof", prof.signed(), Modifier.weight(1f))
            StatChip("Speed", "${speed2024(payload)} ft", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        HPBlock(payload.currentHp, payload.effectiveMaxHp, payload.temporaryHp, compact = true)

        SectionHead("Abilities", top = 28.dp, bottom = 16.dp)
        Ability.entries.chunked(3).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                row.forEach { ability ->
                    val mod = AbilityScores.modifier(scores.score(ability))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AbilityMedallion(
                            abbrev = ability.abbreviation,
                            score = scores.score(ability),
                            modifierValue = mod,
                            onClick = { check = CheckRoll2024("${ability.abbreviation} check", listOf(RollBonus(ability.abbreviation, mod))) },
                        )
                    }
                }
            }
        }

        SectionHead("Saving Throws", top = 24.dp, bottom = 16.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Ability.entries.forEach { ability ->
                val proficient = ability in proficientSaves
                val abilityMod = AbilityScores.modifier(scores.score(ability))
                val effectBonus = payload.temporarySaveBonus(ability)
                val mod = abilityMod + (if (proficient) prof else 0) + effectBonus
                SaveCell(
                    abbrev = ability.abbreviation,
                    bonus = mod,
                    proficient = proficient,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val bonuses = buildList {
                            add(RollBonus(ability.abbreviation, abilityMod))
                            if (proficient) add(RollBonus("Proficiency", prof))
                            if (effectBonus != 0) add(RollBonus("Effects", effectBonus))
                        }
                        check = CheckRoll2024("${ability.abbreviation} save", bonuses)
                    },
                )
            }
        }

        if (payload.activeEffects.isNotEmpty()) {
            SectionHead("Effects", top = 24.dp, bottom = 12.dp)
            EffectFlow2024(payload, onApplyIntent)
        }

        // 2024 conditions: a single cumulative exhaustion line + active conditions.
        if (payload.exhaustionLevel > 0 || payload.activeConditions.isNotEmpty()) {
            SectionHead("Conditions", top = 24.dp, bottom = 12.dp)
            if (payload.exhaustionLevel > 0) {
                Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Text(
                        "Exhaustion ${payload.exhaustionLevel}",
                        fontFamily = Cormorant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = palette.ink,
                    )
                    Text(
                        "${Exhaustion2024.d20Modifier(payload.exhaustionLevel).signed()} to d20 Tests · −${Exhaustion2024.speedPenaltyFeet(payload.exhaustionLevel)} ft Speed",
                        fontFamily = EbGaramond,
                        fontSize = 12.sp,
                        color = palette.inkSoft,
                    )
                }
            }
            if (payload.activeConditions.isNotEmpty()) {
                ConditionChips2024(payload.activeConditions) {}
            }
        }

        // Defenses: 2024 folds species/effect damage resistances (no advantage list).
        val resistances = payload.effectiveDamageResistances.sorted()
        if (resistances.isNotEmpty()) {
            SectionHead("Defenses", top = 24.dp, bottom = 12.dp)
            DefenseCategory2024("Resistance", resistances)
        }

        if (payload.hasInspiration) {
            InspirationBanner(Modifier.padding(top = 14.dp))
        }

        if (payload.level < DnD5e2024Payload.MAX_LEVEL) {
            Spacer(Modifier.height(20.dp))
            CodexButton("Level Up", Modifier.fillMaxWidth(), onClick = onLevelUp)
        }
        Spacer(Modifier.height(8.dp))
    }

    check?.let { CheckRoll2024Dialog(it) { check = null } }
    if (acBreakdown) AcBreakdown2024Dialog(payload) { acBreakdown = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefenseCategory2024(label: String, entries: List<String>) {
    val palette = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.inkMute,
            modifier = Modifier.width(96.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.forEach { entry ->
                val shape = RoundedCornerShape(3.dp)
                Text(
                    entry.replaceFirstChar { it.uppercase() },
                    fontFamily = Cormorant,
                    fontSize = 13.sp,
                    color = palette.accent,
                    modifier = Modifier
                        .clip(shape)
                        .border(1.dp, palette.accent.copy(alpha = 0.4f), shape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

// ── Skills ────────────────────────────────────────────────────────────────────

@Composable
private fun Skills2024(payload: DnD5e2024Payload) {
    val palette = MaterialTheme.natPalette
    val prof = Proficiency.bonus(payload.level)
    val scores = payload.effectiveAbilityScores
    val proficiencies = payload.effectiveSkillProficiencies
    var check by remember { mutableStateOf<CheckRoll2024?>(null) }

    CodexPage {
        val perceptionProficient = "perception" in proficiencies
        val passive = 10 + AbilityScores.modifier(scores.score(Ability.WISDOM)) +
            (if (perceptionProficient) prof else 0) + payload.temporarySkillBonus("perception")
        val shape = RoundedCornerShape(3.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(palette.tileStrong)
                .border(1.dp, palette.accent.copy(alpha = 0.33f), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("PASSIVE PERCEPTION", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.inkMute)
            Spacer(Modifier.weight(1f))
            Text(passive.toString(), fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, color = palette.accent)
        }

        SectionHead("All Skills", top = 24.dp, bottom = 12.dp)
        DnD5eCatalog.skills.forEachIndexed { index, skill ->
            if (index > 0) DashedRule()
            val proficient = skill.id in proficiencies
            val abilityMod = AbilityScores.modifier(scores.score(skill.ability))
            val effectBonus = payload.temporarySkillBonus(skill.id)
            val mod = abilityMod + (if (proficient) prof else 0) + effectBonus
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        val bonuses = buildList {
                            add(RollBonus(skill.ability.abbreviation, abilityMod))
                            if (proficient) add(RollBonus("Proficiency", prof))
                            if (effectBonus != 0) add(RollBonus("Effects", effectBonus))
                        }
                        check = CheckRoll2024("${skill.name} check", bonuses)
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 2024 skills are binary (proficient / untrained) — no expertise tier.
                SkillPip(proficient = proficient, expertise = false)
                Text(
                    skill.name,
                    fontFamily = Cormorant,
                    fontWeight = if (proficient) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 15.sp,
                    color = if (proficient) palette.ink else palette.inkSoft,
                    modifier = Modifier.padding(start = 10.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(skill.ability.abbreviation.uppercase(), fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 1.5.sp, color = palette.inkMute, modifier = Modifier.width(32.dp))
                Text(
                    mod.signed(),
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = if (proficient) palette.accent else palette.ink,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(36.dp),
                )
            }
        }
        Text(
            "○ untrained · ● proficient — tap a skill to roll",
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 11.sp,
            color = palette.inkMute,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    check?.let { CheckRoll2024Dialog(it) { check = null } }
}

// ── Combat ────────────────────────────────────────────────────────────────────

@Composable
private fun Combat2024(payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit) {
    val palette = MaterialTheme.natPalette
    var amount by remember { mutableStateOf<AmountKind?>(null) }
    var rolling by remember { mutableStateOf<RollKind?>(null) }
    var addCond by remember { mutableStateOf(false) }
    var attacking by remember { mutableStateOf(false) }
    var acBreakdown by remember { mutableStateOf(false) }

    val knownWeapons = payload.weaponMasteries.mapNotNull { Weapons2024.weapon(it) }
        .ifEmpty { listOfNotNull(Weapons2024.weapon("dagger")) }

    CodexPage {
        HPBlock(payload.currentHp, payload.effectiveMaxHp, payload.temporaryHp, compact = false)

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Armor", payload.armorClass.toString(), Modifier.weight(1f), onClick = { acBreakdown = true })
            StatChip(
                "Init",
                payload.initiative?.toString() ?: payload.initiativeBonus.signed(),
                Modifier.weight(1f),
                highlight = payload.initiative != null,
                onClick = { if (payload.initiative == null) rolling = RollKind.INITIATIVE else onApplyIntent(SetInitiative2024(null)) },
            )
            StatChip("Speed", "${speed2024(payload)} ft", Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HitDiceTile2024(payload, Modifier.width(130.dp), onClick = { if (payload.currentHitDice > 0) rolling = RollKind.HIT_DIE })
            DeathSavesTile2024(payload, Modifier.weight(1f), onClick = { rolling = RollKind.DEATH_SAVE })
        }

        SectionHead("Attacks", top = 22.dp, bottom = 8.dp)
        Attacks2024Table(payload, knownWeapons)
        Spacer(Modifier.height(10.dp))
        CodexButton(
            if (payload.weapons.isEmpty()) "Add a weapon to attack" else "Make an attack",
            Modifier.fillMaxWidth(),
            enabled = payload.weapons.isNotEmpty(),
            onClick = { attacking = true },
        )

        if (payload.weaponMasteries.isNotEmpty()) {
            SectionHead("Weapon Mastery", top = 22.dp, bottom = 10.dp)
            payload.weaponMasteries.mapNotNull { WeaponMastery2024.from(it) }.forEach { m ->
                FeatureBlock(m.displayName, m.summary)
            }
        }

        payload.fightingStyle?.let { fs ->
            SectionHead("Fighting Style", top = 22.dp, bottom = 10.dp)
            val feat = Feats2024.feat(fs)
            FeatureBlock(feat?.name ?: fs.slugToTitle(), feat?.description.orEmpty())
        }

        if (payload.concentratingOn != null || payload.activeEffects.isNotEmpty()) {
            SectionHead("Active Effects", top = 22.dp, bottom = 10.dp)
            payload.concentratingOn?.let { focus ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Concentrating on ${focus.slugToTitle()}", Modifier.weight(1f), fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.accent)
                    TextButton(onClick = { onApplyIntent(EndConcentration2024()) }) { Text("End") }
                }
            }
            EffectFlow2024(payload, onApplyIntent)
        }

        // ── Android interactive extras (no 2024 actions layer yet — pending #31) ──
        SectionHead("Vitals", top = 22.dp, bottom = 10.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CodexButton("Damage", Modifier.weight(1f), danger = true, onClick = { amount = AmountKind.DAMAGE })
            CodexButton("Heal", Modifier.weight(1f), onClick = { amount = AmountKind.HEAL })
            CodexButton("Temp HP", Modifier.weight(1f), onClick = { amount = AmountKind.TEMP })
        }

        SectionHead("Exhaustion & Inspiration", top = 22.dp, bottom = 10.dp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Exhaustion ${payload.exhaustionLevel} / ${Exhaustion2024.MAX}", fontFamily = Cormorant, fontSize = 15.sp, color = palette.ink)
                if (payload.exhaustionLevel > 0) {
                    Text(
                        "${Exhaustion2024.d20Modifier(payload.exhaustionLevel).signed()} to d20 · −${Exhaustion2024.speedPenaltyFeet(payload.exhaustionLevel)} ft",
                        fontFamily = EbGaramond, fontSize = 12.sp, color = palette.inkSoft,
                    )
                }
            }
            StepperGlyph("−", enabled = payload.exhaustionLevel > 0) { onApplyIntent(ChangeExhaustion2024(-1)) }
            Spacer(Modifier.width(10.dp))
            StepperGlyph("+", enabled = payload.exhaustionLevel < Exhaustion2024.MAX) { onApplyIntent(ChangeExhaustion2024(1)) }
        }
        Spacer(Modifier.height(8.dp))
        CodexButton(
            if (payload.hasInspiration) "Spend Heroic Inspiration" else "Grant Heroic Inspiration",
            Modifier.fillMaxWidth(),
            onClick = { onApplyIntent(SetInspiration2024(!payload.hasInspiration)) },
        )

        SectionHead("Conditions", top = 22.dp, bottom = 10.dp)
        if (payload.activeConditions.isEmpty()) {
            Text("No conditions.", fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkMute)
        } else {
            ConditionChips2024(payload.activeConditions) { onApplyIntent(ClearCondition2024(it)) }
        }
        Spacer(Modifier.height(8.dp))
        CodexButton("Add condition", Modifier.fillMaxWidth(), onClick = { addCond = true })

        SectionHead("Rest", top = 22.dp, bottom = 10.dp)
        CodexButton("Long rest", Modifier.fillMaxWidth(), onClick = { onApplyIntent(LongRest2024()) })
        Spacer(Modifier.height(8.dp))
    }

    amount?.let { kind ->
        AmountDialog(kind.label, onConfirm = { n ->
            onApplyIntent(when (kind) { AmountKind.DAMAGE -> TakeDamage2024(n); AmountKind.HEAL -> Heal2024(n); AmountKind.TEMP -> GainTempHp2024(n) })
            amount = null
        }, onDismiss = { amount = null })
    }
    rolling?.let { kind ->
        val dexMod = AbilityScores.modifier(payload.effectiveScore(Ability.DEXTERITY))
        val con = AbilityScores.modifier(payload.effectiveScore(Ability.CONSTITUTION))
        when (kind) {
            RollKind.DEATH_SAVE -> RollDialog("Death save", RollSpec.d(1, 20), allowAdvantageToggle = false, onSettled = { it.naturalD20?.let { d -> onApplyIntent(RollDeathSave2024(d)) } }, onDismiss = { rolling = null })
            RollKind.INITIATIVE -> {
                val alertProf = payload.initiativeBonus - dexMod
                RollDialog("Initiative", RollSpec.d(1, 20), bonuses = buildList { add(RollBonus("DEX", dexMod)); if (alertProf != 0) add(RollBonus("Alert", alertProf)) }, onSettled = { onApplyIntent(SetInitiative2024(it.total)) }, onDismiss = { rolling = null })
            }
            RollKind.HIT_DIE -> RollDialog("Spend a hit die", RollSpec.d(1, hitDie2024(payload)), bonuses = if (con != 0) listOf(RollBonus("CON", con)) else emptyList(), allowAdvantageToggle = false, onSettled = { onApplyIntent(SpendHitDie2024(maxOf(1, it.total))) }, onDismiss = { rolling = null })
        }
    }
    if (addCond) ConditionPickerDialog(payload.activeConditions, onPick = { onApplyIntent(ApplyCondition2024(it)); addCond = false }, onDismiss = { addCond = false })
    if (attacking) AttackSheet2024(payload, onApplyIntent = { onApplyIntent(it); attacking = false }, onDismiss = { attacking = false })
    if (acBreakdown) AcBreakdown2024Dialog(payload) { acBreakdown = false }
}

@Composable
private fun Attacks2024Table(payload: DnD5e2024Payload, weapons: List<au.com.evonet.nat20.dnd5e2024.Weapon2024>) {
    val palette = MaterialTheme.natPalette
    if (weapons.isEmpty()) {
        DashedNotice("No weapons — add one on the Items tab.")
        return
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            AttackHeader2024("WEAPON", Modifier.weight(1f))
            AttackHeader2024("ATK", Modifier.width(44.dp))
            AttackHeader2024("DAMAGE", Modifier.width(96.dp))
        }
        weapons.forEach { weapon ->
            HairlineRule()
            val attack = AttackMath2024.forWeapon(weapon, payload)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(weapon.name, fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = palette.ink, maxLines = 1)
                    Text(
                        "${attack.abilityLabel.lowercase()} · ${weapon.mastery.displayName}",
                        fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 11.sp, color = palette.inkMute, maxLines = 1,
                    )
                }
                Text(attack.attackBonuses.sumOf { it.value }.signed(), fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = palette.accent, modifier = Modifier.width(44.dp))
                Text(weapon.damage, fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = palette.ink, modifier = Modifier.width(96.dp))
            }
        }
        HairlineRule()
    }
}

@Composable
private fun AttackHeader2024(text: String, modifier: Modifier) {
    Text(text, fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 1.8.sp, color = MaterialTheme.natPalette.inkMute, modifier = modifier)
}

@Composable
private fun HitDiceTile2024(payload: DnD5e2024Payload, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    Column(
        modifier.clip(shape).background(palette.tile).border(1.dp, palette.accent.copy(alpha = 0.2f), shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("HIT DICE", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.inkMute)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(payload.currentHitDice.toString(), fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = palette.accent, modifier = Modifier.alignByBaseline())
            Text("/${payload.maxHitDice}d${hitDie2024(payload)}", fontFamily = Cormorant, fontSize = 12.sp, color = palette.inkSoft.copy(alpha = 0.6f), modifier = Modifier.alignByBaseline())
        }
    }
}

@Composable
private fun DeathSavesTile2024(payload: DnD5e2024Payload, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val ds = payload.deathSaves
    val shape = RoundedCornerShape(3.dp)
    Column(
        modifier.clip(shape).background(palette.tile).border(1.dp, palette.accent.copy(alpha = 0.2f), shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("DEATH SAVES", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.inkMute)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { i ->
                Box(Modifier.size(9.dp).clip(CircleShape).then(if (i < ds.successes) Modifier.background(palette.accent) else Modifier.border(1.dp, palette.accent.copy(alpha = 0.53f), CircleShape)))
            }
            Spacer(Modifier.width(8.dp))
            repeat(3) { i ->
                Box(Modifier.size(9.dp).clip(DiamondShape).then(if (i < ds.failures) Modifier.background(palette.danger) else Modifier.border(1.dp, palette.danger.copy(alpha = 0.5f), DiamondShape)))
            }
        }
    }
}

@Composable
private fun StepperGlyph(label: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    Box(
        Modifier.size(34.dp).clip(shape).background(palette.tile)
            .border(1.dp, palette.accent.copy(alpha = if (enabled) 0.4f else 0.15f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = Cormorant, fontSize = 20.sp, color = if (enabled) palette.accent else palette.inkMute)
    }
}

// ── Spells ────────────────────────────────────────────────────────────────────

@Composable
private fun Spells2024(character: Character, payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit, onSave: (Character) -> Unit) {
    val palette = MaterialTheme.natPalette
    val primary = payload.classes.firstOrNull { CastingProgression.forClass(it.classId) != CastingProgression.NONE }
    var browse by remember { mutableStateOf<Boolean?>(null) } // null=closed; true=cantrip add; false=prepared add
    var library by remember { mutableStateOf(false) }
    var spellAction by remember { mutableStateOf<Pair<Spell2024, Boolean>?>(null) } // spell, isCantrip

    CodexPage {
        if (primary == null) {
            NoSpells2024EmptyState("${payload.classes.firstOrNull()?.classId?.slugToTitle() ?: "This class"} channels no magic — the catalogue is still open to the curious.") { library = true }
            return@CodexPage
        }
        val classId = primary.classId
        val cantrips = payload.cantripsKnown.mapNotNull { DnD5e2024Catalog.spell(it) }.sortedBy { it.name }
        val preparedIds = payload.preparedSpells[classId].orEmpty()
        val prepared = preparedIds.mapNotNull { DnD5e2024Catalog.spell(it) }.sortedWith(compareBy({ it.level }, { it.name }))

        CodexButton("Browse full Spell Library", Modifier.fillMaxWidth(), onClick = { library = true })

        payload.concentratingOn?.let { focus ->
            Text(
                "Concentrating on ${focus.slugToTitle()}",
                fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.accent,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // Spell slots.
        val maxSlots = payload.maxSpellSlots
        SectionHead("Spell Slots", top = 22.dp, bottom = 10.dp)
        if (maxSlots.isEmpty()) {
            DashedNotice("No spell slots at this level.")
        } else {
            SlotFlow2024(payload, onApplyIntent)
        }

        SectionHead("Cantrips · ${cantrips.size}", top = 22.dp, bottom = 10.dp)
        if (cantrips.isEmpty()) {
            DashedNotice("No cantrips known yet.")
            Spacer(Modifier.height(8.dp))
        } else {
            CantripPills2024(cantrips) { spellAction = it to true }
            Spacer(Modifier.height(10.dp))
        }
        CodexButton("Add cantrip", onClick = { browse = true })

        SectionHead("Prepared · ${prepared.size}", top = 22.dp, bottom = 10.dp)
        if (prepared.isEmpty()) {
            DashedNotice("No spells prepared.")
            Spacer(Modifier.height(10.dp))
        } else {
            prepared.forEachIndexed { i, s ->
                if (i > 0) DashedRule()
                SpellRow2024(s, payload) { spellAction = s to false }
            }
            Spacer(Modifier.height(10.dp))
        }
        CodexButton("Prepare a spell", onClick = { browse = false })
        Spacer(Modifier.height(8.dp))
    }

    browse?.let { cantripMode ->
        val have = if (cantripMode) payload.cantripsKnown.toSet() else payload.preparedSpells.values.flatten().toSet()
        AddSpell2024Dialog(
            cantrips = cantripMode,
            alreadyHave = have,
            onPick = { picked ->
                val cid = primary?.classId ?: "wizard"
                if (cantripMode) {
                    // Cantrips known is a list edit (no journaled intent for cantrips).
                    onSave(character.copy(payload = payload.copy(cantripsKnown = (payload.cantripsKnown + picked.id).distinct())))
                } else {
                    onApplyIntent(PrepareSpell2024(picked.id, picked.name, cid))
                }
                browse = null
            },
            onDismiss = { browse = null },
        )
    }
    spellAction?.let { (s, isCantrip) ->
        val hasSlot = isCantrip || (s.level..9).any { (payload.currentSpellSlots[it] ?: 0) > 0 }
        AlertDialog(
            onDismissRequest = { spellAction = null },
            title = { Text(s.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${s.levelLabel} · ${s.school}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!isCantrip && !hasSlot) Text("No level-${s.level}+ slots remaining.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(enabled = hasSlot, onClick = {
                    onApplyIntent(CastSpell2024(s.id, s.name, if (isCantrip) 0 else s.level, if (isCantrip) 0 else s.level, requiresConcentration = s.concentration, applyToSelf = true))
                    spellAction = null
                }) { Text("Cast") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val cid = primary?.classId ?: "wizard"
                        if (isCantrip) onSave(character.copy(payload = payload.copy(cantripsKnown = payload.cantripsKnown - s.id)))
                        else onApplyIntent(UnprepareSpell2024(s.id, s.name, cid))
                        spellAction = null
                    }) { Text(if (isCantrip) "Remove" else "Unprepare") }
                    TextButton(onClick = { spellAction = null }) { Text("Close") }
                }
            },
        )
    }
    if (library) SpellLibrary2024Dialog { library = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotFlow2024(payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit) {
    val max = payload.maxSpellSlots
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        max.keys.sorted().forEach { lvl ->
            val remaining = payload.currentSpellSlots[lvl] ?: 0
            SlotTile(lvl, remaining, max.getValue(lvl), onClick = { if (remaining > 0) onApplyIntent(ExpendSpellSlot2024(lvl)) })
        }
    }
    Text(
        "Tap a tile to expend a slot; long rest on the Combat tab to recover.",
        fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 11.sp, color = MaterialTheme.natPalette.inkMute,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CantripPills2024(cantrips: List<Spell2024>, onTap: (Spell2024) -> Unit) {
    val palette = MaterialTheme.natPalette
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cantrips.forEach { s ->
            val shape = RoundedCornerShape(50)
            Text(
                s.name,
                fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.ink,
                modifier = Modifier.clip(shape).border(1.dp, palette.accent.copy(alpha = 0.33f), shape).clickable { onTap(s) }.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SpellRow2024(spell: Spell2024, payload: DnD5e2024Payload, onTap: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth().clickable(onClick = onTap).padding(vertical = 8.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Diamond(size = 8.dp, fill = palette.accent)
        Text(spell.name, fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = palette.ink, modifier = Modifier.padding(start = 10.dp))
        Spacer(Modifier.weight(1f))
        Text("LVL ${spell.level}", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 1.5.sp, color = palette.inkMute)
    }
}

@Composable
private fun NoSpells2024EmptyState(subtitle: String, onBrowse: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(Modifier.fillMaxSize().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No spells known.", fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 22.sp, color = palette.ink)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkMute, textAlign = TextAlign.Center, modifier = Modifier.width(240.dp))
        Spacer(Modifier.height(18.dp))
        CodexButton("Browse the Catalogue", onClick = onBrowse)
    }
}

// ── Items ─────────────────────────────────────────────────────────────────────

@Composable
private fun Items2024(payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit) {
    val palette = MaterialTheme.natPalette
    var pickArmor by remember { mutableStateOf(false) }
    var addItem by remember { mutableStateOf(false) }
    var editMasteries by remember { mutableStateOf(false) }
    var adjustCoin by remember { mutableStateOf<Coin?>(null) }
    val masteryCap = payload.classes.maxOfOrNull { WeaponMasteryProgression2024.slots(it.classId, it.level) } ?: 0

    CodexPage {
        // Purse: five coin tiles, tap to adjust.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Coin.entries.forEach { coin ->
                CoinTile(coin.abbreviation, payload.coins[coin] ?: 0, Modifier.weight(1f)) { adjustCoin = coin }
            }
        }

        // Armor & shield (2024-specific gear editor — kept, restyled).
        SectionHead("Armor & Shield", top = 22.dp, bottom = 10.dp)
        val breakdown = ArmorClass2024.compute(payload)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("Armor Class", Modifier.weight(1f), fontFamily = Cormorant, fontSize = 15.sp, color = palette.inkSoft)
            Text(breakdown.total.toString(), fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = palette.accent)
        }
        Text(breakdown.rows.joinToString("  ·  ") { "${it.label} ${it.value.signed()}" }, fontFamily = EbGaramond, fontSize = 12.sp, color = palette.inkSoft, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(10.dp))
        GearRow2024(payload.equippedArmor?.let { Armors2024.armor(it)?.name } ?: "Unarmored", "Change") { pickArmor = true }
        GearRow2024(if (payload.hasShield) "Shield equipped (+${Armors2024.SHIELD_BONUS})" else "No shield", if (payload.hasShield) "Stow" else "Equip", highlight = payload.hasShield) { onApplyIntent(SetShield2024(!payload.hasShield)) }

        // Weapon masteries editor (kept, restyled).
        SectionHead("Weapon Masteries", top = 22.dp, bottom = 10.dp)
        Text("Your class grants $masteryCap mastery slot${if (masteryCap == 1) "" else "s"}.", fontFamily = EbGaramond, fontSize = 12.sp, color = palette.inkSoft)
        if (payload.weaponMasteries.isEmpty()) {
            Text("None chosen.", fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkMute, modifier = Modifier.padding(vertical = 4.dp))
        } else {
            payload.weaponMasteries.mapNotNull { WeaponMastery2024.from(it) }.forEach { FeatureBlock(it.displayName, it.summary) }
        }
        if (masteryCap > 0) {
            Spacer(Modifier.height(6.dp))
            CodexButton("Edit masteries", onClick = { editMasteries = true })
        }

        // The pack: kind.displayName rows, drop via DropItem2024.
        SectionHead("Inventory", top = 22.dp, bottom = 12.dp)
        CodexCapsuleButton("Add Item", onClick = { addItem = true })
        if (payload.inventory.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            DashedNotice("Your pack is empty. Add gear from the SRD catalogue.")
        } else {
            val groups = payload.inventory.groupBy { it.kind }
            ItemKind2024.entries.forEach { kind ->
                val items = groups[kind].orEmpty()
                if (items.isEmpty()) return@forEach
                Text(
                    kind.displayName.uppercase() + if (items.size > 1) " (${items.size})" else "",
                    fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.accent,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                items.forEach { item -> ItemRow2024(item, kind) { onApplyIntent(DropItem2024(item.id, item.quantity)) } }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (pickArmor) ArmorPicker2024Dialog(payload.equippedArmor, onPick = { onApplyIntent(EquipArmor2024(it)); pickArmor = false }, onDismiss = { pickArmor = false })
    if (addItem) AddItem2024Dialog(onAdd = { onApplyIntent(AcquireItem2024(it)); addItem = false }, onDismiss = { addItem = false })
    if (editMasteries) MasteryPicker2024Dialog(payload.weaponMasteries, masteryCap, onConfirm = { onApplyIntent(SetWeaponMasteries2024(it)); editMasteries = false }, onDismiss = { editMasteries = false })
    adjustCoin?.let { coin ->
        CoinAdjust2024Dialog(coin, payload.coins[coin] ?: 0, onConfirm = { delta -> if (delta != 0) onApplyIntent(AdjustCoin2024(coin, delta)); adjustCoin = null }, onDismiss = { adjustCoin = null })
    }
}

@Composable
private fun GearRow2024(label: String, action: String, highlight: Boolean = false, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontFamily = Cormorant, fontSize = 15.sp, color = if (highlight) palette.accent else palette.ink)
        CodexButton(action, onClick = onClick)
    }
}

@Composable
private fun ItemRow2024(item: InventoryItem2024, kind: ItemKind2024, onDrop: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Diamond(size = 5.dp, fill = palette.accent)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(if (item.quantity > 1) "${item.name} × ${item.quantity}" else item.name, fontFamily = Cormorant, fontSize = 15.sp, color = palette.ink, maxLines = 1)
            Text(kind.displayName, fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 11.sp, color = palette.inkMute)
        }
        TextButton(onClick = onDrop) { Text("Drop") }
    }
}

// ── Lore ──────────────────────────────────────────────────────────────────────

@Composable
private fun Lore2024(character: Character, payload: DnD5e2024Payload) {
    val palette = MaterialTheme.natPalette
    CodexPage {
        // Identity grid.
        Row(Modifier.fillMaxWidth()) {
            au.com.evonet.nat20.ui.codex.IdentityCell("Species", speciesLine2024(payload).takeIf { payload.species.isNotEmpty() }, Modifier.weight(1f))
            au.com.evonet.nat20.ui.codex.IdentityCell("Background", payload.background.takeIf { it.isNotEmpty() }?.slugToTitle(), Modifier.weight(1f))
        }

        payload.originFeat?.let { feat ->
            SectionHead("Origin Feat", top = 22.dp, bottom = 10.dp)
            val f = Feats2024.feat(feat)
            FeatureBlock(f?.name ?: feat.slugToTitle(), f?.description.orEmpty())
        }

        val speciesTraits = SpeciesTraits2024.reminders(payload.species)
        val traitNames = DnD5e2024Catalog.species(payload.species)?.traits.orEmpty()
        if (speciesTraits.isNotEmpty() || traitNames.isNotEmpty()) {
            SectionHead("Species Traits", top = 22.dp, bottom = 10.dp)
            if (speciesTraits.isNotEmpty()) {
                speciesTraits.forEach { FeatureBlock(it.title, it.detail) }
            } else {
                traitNames.forEach { FeatureBlock(it, "") }
            }
        }

        if (payload.chosenFeats.isNotEmpty() || payload.fightingStyle != null) {
            SectionHead("Feats & Style", top = 22.dp, bottom = 10.dp)
            payload.fightingStyle?.let { val f = Feats2024.feat(it); FeatureBlock(f?.name ?: it.slugToTitle(), f?.description.orEmpty()) }
            payload.chosenFeats.forEach { val f = Feats2024.feat(it); FeatureBlock(f?.name ?: it.slugToTitle(), f?.description.orEmpty()) }
        }

        payload.alignment?.takeIf { it.isNotEmpty() }?.let {
            SectionHead("Alignment", top = 22.dp, bottom = 10.dp)
            Text(it, fontFamily = Cormorant, fontSize = 15.sp, color = palette.ink)
        }
        // NOTE: the iOS 2024 Lore page also renders a Backstory block; the Android
        // DnD5e2024Payload has no `backstory` field yet (payload gap — defer).
        Spacer(Modifier.height(8.dp))
    }
}

// ── Shared effect / condition helpers ─────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EffectFlow2024(payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit) {
    if (payload.activeEffects.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        payload.activeEffects.forEach { e -> CodexEffectChip(e) { onApplyIntent(CancelEffect2024(e.id)) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionChips2024(conditions: List<String>, onClear: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        conditions.forEach { AssistChip(onClick = { onClear(it) }, label = { Text(it) }) }
    }
}

// ── Dialogs (kept functional; restyle is deferred to slice 2/3) ───────────────

@Composable
private fun ArmorPicker2024Dialog(current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose armor") },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item {
                    ArmorPickRow("Unarmored", "10 + DEX", current == null) { onPick(null) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
                items(Armors2024.all, key = { it.id }) { a ->
                    val cap = when (a.category.dexCap) { null -> "+ full DEX"; 0 -> "no DEX"; else -> "+ DEX (max +${a.category.dexCap})" }
                    ArmorPickRow(a.name, "${a.category.name.lowercase().replaceFirstChar(Char::uppercase)} · base ${a.baseAC} $cap", current == a.id) { onPick(a.id) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun ArmorPickRow(name: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (selected) "●" else "○", color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddItem2024Dialog(onAdd: (InventoryItem2024) -> Unit, onDismiss: () -> Unit) {
    var custom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to pack") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Weapons", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(Weapons2024.all, key = { it.id }) { w ->
                        Row(Modifier.fillMaxWidth().clickable {
                            onAdd(InventoryItem2024(InventoryItem2024.newId(), w.name, kind = ItemKind2024.WEAPON, catalogueID = w.id))
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(w.name, style = MaterialTheme.typography.bodyLarge)
                                Text("${w.damage} · ${w.mastery.displayName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(w.category.name.lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                OutlinedTextField(custom, { custom = it }, label = { Text("Custom item (gear)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (custom.isNotBlank()) TextButton(onClick = { onAdd(InventoryItem2024(InventoryItem2024.newId(), custom.trim(), kind = ItemKind2024.GEAR)) }) { Text("Add \"${custom.trim()}\"") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MasteryPicker2024Dialog(current: List<String>, cap: Int, onConfirm: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val selected = remember { mutableStateOf(current.mapNotNull { WeaponMastery2024.from(it)?.name?.lowercase() }.toMutableList()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weapon masteries ($cap)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pick up to $cap.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    WeaponMastery2024.entries.forEach { m ->
                        val key = m.name.lowercase()
                        val on = key in selected.value
                        AssistChip(
                            onClick = {
                                val next = selected.value.toMutableList()
                                if (on) next.remove(key) else if (next.size < cap) next.add(key)
                                selected.value = next
                            },
                            label = { Text(m.displayName) },
                            leadingIcon = if (on) ({ Text("✓", style = MaterialTheme.typography.labelMedium) }) else null,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected.value.toList()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CoinAdjust2024Dialog(coin: Coin, current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var delta by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust ${coin.fullName}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Have $current ${coin.abbreviation}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(enabled = current + delta > 0, onClick = { delta-- }) { Text("−", style = MaterialTheme.typography.headlineSmall) }
                    Text((if (delta >= 0) "+$delta" else "$delta"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { delta++ }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
                }
                Text("New total: ${current + delta}", style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(delta) }) { Text("Apply") } },
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
        title = { Text(if (cantrips) "Add a cantrip" else "Prepare a spell") },
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
private fun AcBreakdown2024Dialog(payload: DnD5e2024Payload, onDismiss: () -> Unit) {
    val breakdown = ArmorClass2024.compute(payload)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Armor Class ${breakdown.total}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                breakdown.rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(row.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(row.value.signed(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

// ── Roll dialog + small enums / helpers ───────────────────────────────────────

private data class CheckRoll2024(val title: String, val bonuses: List<RollBonus>)

@Composable
private fun CheckRoll2024Dialog(check: CheckRoll2024, onDismiss: () -> Unit) {
    RollDialog(check.title, RollSpec.d(1, 20), check.bonuses, onDismiss = onDismiss)
}

private enum class AmountKind(val label: String) { DAMAGE("Damage"), HEAL("Heal"), TEMP("Temp HP") }
private enum class RollKind { DEATH_SAVE, INITIATIVE, HIT_DIE }

private fun speciesLine2024(payload: DnD5e2024Payload): String {
    val resolved = DnD5e2024Catalog.species(payload.species)?.name
        ?: payload.species.takeIf { it.isNotEmpty() }?.slugToTitle()
    return resolved ?: "Unknown species"
}

private fun classAndLevelLine2024(payload: DnD5e2024Payload): String {
    val primary = payload.classes.firstOrNull()?.classId
        ?.let { DnD5e2024Catalog.classes.firstOrNull { c -> c.id == it }?.name ?: it.slugToTitle() }
        ?: "Adventurer"
    return "Level ${payload.level} $primary"
}

private fun speed2024(payload: DnD5e2024Payload): Int =
    DnD5e2024Catalog.species(payload.species)?.speed ?: 30

private fun hitDie2024(payload: DnD5e2024Payload): Int =
    au.com.evonet.nat20.dnd5e.core.DnD5eClasses.hitDie(payload.classes.firstOrNull()?.classId ?: "")
