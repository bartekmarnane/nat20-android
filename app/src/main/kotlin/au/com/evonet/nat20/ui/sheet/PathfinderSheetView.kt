package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.pf2e.Bulk
import au.com.evonet.nat20.pf2e.PFCoin
import au.com.evonet.nat20.pf2e.PathfinderCatalog
import au.com.evonet.nat20.pf2e.PathfinderConditions
import au.com.evonet.nat20.pf2e.PathfinderPayload
import au.com.evonet.nat20.pf2e.PfArmors
import au.com.evonet.nat20.pf2e.PfFeats
import au.com.evonet.nat20.pf2e.PfShields
import au.com.evonet.nat20.pf2e.PfSpells
import au.com.evonet.nat20.pf2e.Wealth
import au.com.evonet.nat20.pf2e.armorClass
import au.com.evonet.nat20.pf2e.armorClassRaised
import au.com.evonet.nat20.pf2e.classDcValue
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save
import au.com.evonet.nat20.pf2e.encumberedThreshold
import au.com.evonet.nat20.pf2e.isEncumbered
import au.com.evonet.nat20.pf2e.loreBonus
import au.com.evonet.nat20.pf2e.maxSpellSlots
import au.com.evonet.nat20.pf2e.perceptionBonus
import au.com.evonet.nat20.pf2e.saveBonus
import au.com.evonet.nat20.pf2e.skillBonus
import au.com.evonet.nat20.pf2e.spellAttack
import au.com.evonet.nat20.pf2e.spellDc
import au.com.evonet.nat20.pf2e.strikes
import au.com.evonet.nat20.pf2e.totalBulk
import au.com.evonet.nat20.ui.actions.PathfinderActionsLayer
import au.com.evonet.nat20.ui.codex.CodexPage
import au.com.evonet.nat20.ui.codex.SectionHead
import au.com.evonet.nat20.ui.codex.signed
import au.com.evonet.nat20.ui.identity.IdentitySheet
import au.com.evonet.nat20.ui.codex.CodexScaffold
import au.com.evonet.nat20.ui.slugToTitle
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette
import java.time.Instant

/**
 * The Pathfinder 2e (Remaster) character sheet (parity #34): a single **read-only
 * scroll** on the shared parchment [CodexScaffold] (nav row, hero, campaign
 * region — no tab bar), rebuilt from the old 6-tab interactive pager to match the
 * iOS `CodexPathfinderShellView`. Every mutation the old tabs fired inline now
 * lives behind the in-campaign Act pill in the [PathfinderActionsLayer] (#35), so
 * the scroll is pure display: vitals, the wearing line, strikes, abilities/saves/
 * skills with U-T-E-M-L rank letters, spellcasting + focus with action glyphs,
 * feats, inventory, and conditions.
 */
@Composable
fun PathfinderSheetView(
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
    val payload = character.payload as? PathfinderPayload ?: return
    val inCampaign = character.phase is CharacterPhase.InCampaign
    val pagerState = rememberPagerState(pageCount = { 1 })
    var showActions by remember { mutableStateOf(false) }
    var editingIdentity by remember { mutableStateOf(false) }

    CodexScaffold(
        pagerState = pagerState,
        tabLabels = listOf("Character"),
        heroName = character.name,
        heroSubtitleLines = listOf(pfHeroLine(payload)),
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
        showTabBar = false,
        modifier = modifier,
    ) {
        PathfinderScroll(payload)
    }

    PathfinderActionsLayer(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PathfinderScroll(payload: PathfinderPayload) {
    val palette = MaterialTheme.natPalette
    CodexPage {
        // 1. VITALS — a wrapping row of stat cells (PF2e-only cells included).
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PfStatCell("HP", "${payload.currentHp}/${payload.maxHp}" + if (payload.temporaryHp > 0) " (+${payload.temporaryHp})" else "")
            PfStatCell("AC", payload.armorClass.toString())
            payload.armorClassRaised?.let { PfStatCell("AC raised", it.toString()) }
            PfStatCell("Perception", payload.perceptionBonus.signed())
            PfStatCell("Class DC", payload.classDcValue.toString())
            PfStatCell("Hero Pts", payload.heroPoints.toString())
            PfStatCell("Bulk", "${Bulk.effective(payload.totalBulk)}/${payload.encumberedThreshold}")
            if (payload.dying >= 1) PfStatCell("Dying", "${payload.dying}/${PathfinderPayload.DYING_MAX}", danger = true)
            if (payload.wounded >= 1) PfStatCell("Wounded", payload.wounded.toString(), danger = true)
            if (payload.isEncumbered) EncumberedChip()
        }

        // 2. WEARING line.
        pfWearingLine(payload)?.let { line ->
            Text(
                line,
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = palette.inkMute,
                modifier = Modifier.padding(top = 14.dp),
            )
            payload.armorRunes.properties.mapNotNull { au.com.evonet.nat20.pf2e.Runes.propertyRune(it) }.forEach { r ->
                Text(
                    "${r.name} — ${r.summary}",
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = 11.sp,
                    color = palette.inkMute,
                )
            }
        }

        // 3. STRIKES.
        val strikes = payload.strikes
        if (strikes.isNotEmpty()) {
            SectionHead("Strikes")
            strikes.forEach { s ->
                val runes = payload.weaponRunes[s.weapon.id]
                val name = s.weapon.name + (runes?.takeIf { !it.isEmpty }?.let { " (+${it.potency}${if (it.striking > 0) " striking" else ""})" } ?: "")
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f), fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = palette.ink)
                    Text(s.attackMods.first().signed(), fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = palette.accent, modifier = Modifier.padding(horizontal = 10.dp))
                    Text("${s.damage} ${s.damageType}", fontFamily = Cormorant, fontSize = 13.sp, color = palette.ink, modifier = Modifier.widthIn(min = 84.dp))
                }
            }
        }

        // 4. ABILITIES.
        SectionHead("Abilities")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PfAbility.entries.forEach { a -> PfAbilityTile(a.abbreviation, payload.abilityScores.score(a)) }
        }

        // 5. SAVING THROWS.
        SectionHead("Saving Throws")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Save.entries.forEach { s -> PfStatCell(s.displayName, payload.saveBonus(s).signed()) }
        }

        // 6. SKILLS — trained+ only, alphabetical, with the rank letter.
        val trained = PfSkill.entries.filter { (payload.skills[it] ?: Proficiency.UNTRAINED) != Proficiency.UNTRAINED }
            .sortedBy { it.displayName }
        if (trained.isNotEmpty() || payload.loreSkills.isNotEmpty()) {
            SectionHead("Skills")
            trained.forEach { skill ->
                PfSkillRow(skill.displayName, payload.skills[skill] ?: Proficiency.UNTRAINED, payload.skillBonus(skill))
            }
            payload.loreSkills.toSortedMap().forEach { (subtype, rank) ->
                PfSkillRow("$subtype Lore", rank, payload.loreBonus(subtype))
            }
        }

        // 7. SPELLCASTING.
        if (payload.spellTradition != null) {
            SectionHead("Spellcasting")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PfStatCell("Tradition", payload.spellTradition!!.displayName)
                PfStatCell("Spell Atk", payload.spellAttack.signed())
                PfStatCell("Spell DC", payload.spellDc.toString())
            }
            val max = payload.maxSpellSlots
            if (max.isNotEmpty()) {
                Text(
                    "Slots — " + max.keys.sorted().joinToString(" · ") { "Rank $it: ${payload.currentSpellSlots[it] ?: 0}/${max.getValue(it)}" },
                    fontFamily = EbGaramond, fontSize = 13.sp, color = palette.inkSoft, modifier = Modifier.padding(top = 8.dp),
                )
            }
            pfSpellLine("Cantrips", payload.cantrips)
            payload.knownSpells.keys.sorted().forEach { rank ->
                pfSpellLine("Rank $rank", payload.knownSpells[rank].orEmpty())
            }
        }

        // 8. FOCUS.
        if (payload.maxFocusPoints > 0) {
            SectionHead("Focus")
            Text(
                "Focus Points ${payload.focusPoints}/${payload.maxFocusPoints}",
                fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.accent,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            payload.focusSpells.mapNotNull { PfSpells.by(it) }.forEach { s ->
                Column(Modifier.padding(vertical = 3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(threeActionGlyph(s.actions), fontFamily = Cormorant, fontSize = 15.sp, color = palette.accent, modifier = Modifier.padding(end = 8.dp))
                        Text(s.name, fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = palette.ink)
                    }
                    if (s.summary.isNotBlank()) Text(s.summary, fontFamily = EbGaramond, fontSize = 13.sp, color = palette.inkSoft)
                }
            }
        }

        // 9. FEATS.
        if (payload.feats.isNotEmpty()) {
            SectionHead("Feats")
            payload.feats.mapNotNull { PfFeats.by(it) }.forEach { f ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(f.name, fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp, color = palette.ink)
                        Spacer(Modifier.width(8.dp))
                        Text(f.type.displayName, fontFamily = EbGaramond, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = palette.inkMute)
                    }
                    if (f.summary.isNotBlank()) Text(f.summary, fontFamily = EbGaramond, fontSize = 13.sp, color = palette.inkSoft)
                }
            }
        }

        // 10. INVENTORY.
        if (payload.coins.isNotEmpty() || payload.inventory.isNotEmpty()) {
            SectionHead("Inventory")
            Text(
                "Purse: ${Wealth.purseLabel(payload.coins)}",
                fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.accentGold,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            payload.inventory.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, Modifier.weight(1f), fontFamily = Cormorant, fontSize = 15.sp, color = palette.ink)
                    if (item.quantity > 1) Text("×${item.quantity}", fontFamily = Cinzel, fontSize = 11.sp, color = palette.inkMute)
                }
            }
        }

        // 11. CONDITIONS.
        if (payload.conditions.isNotEmpty()) {
            SectionHead("Conditions")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                payload.conditions.forEach { c -> PfConditionCapsule(c.label(PathfinderConditions.displayName(c.id))) }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

// ── PF2e atoms ───────────────────────────────────────────────────────────────

/** Label-over-value stat cell (min 72dp), danger-tinted for the dying/wounded track. */
@Composable
private fun PfStatCell(label: String, value: String, danger: Boolean = false) {
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(4.dp)
    val tone = if (danger) p.danger else p.ink
    Column(
        Modifier
            .widthIn(min = 72.dp)
            .clip(shape)
            .background(p.tile)
            .border(1.dp, (if (danger) p.danger else p.accent).copy(alpha = 0.2f), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label.uppercase(), fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 1.5.sp, color = p.inkMute, maxLines = 1)
        Text(value, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = tone, maxLines = 1)
    }
}

/** Encumbered / over-capacity danger chip. */
@Composable
private fun EncumberedChip() {
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(4.dp)
    Text(
        "ENCUMBERED",
        fontFamily = Cinzel,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
        color = p.cream,
        modifier = Modifier
            .clip(shape)
            .background(p.danger)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

/** 54dp ability tile: abbrev / score / modifier. */
@Composable
private fun PfAbilityTile(abbrev: String, score: Int) {
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(4.dp)
    Column(
        Modifier
            .width(54.dp)
            .clip(shape)
            .background(p.tile)
            .border(1.dp, p.accent.copy(alpha = 0.2f), shape)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(abbrev.uppercase(), fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 1.sp, color = p.inkMute)
        Text(score.toString(), fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = p.ink)
        Text(PfAbilityScores.modifier(score).signed(), fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 11.sp, color = p.accent)
    }
}

/** Skill / save / lore row: name / U-T-E-M-L rank letter / modifier. */
@Composable
private fun PfSkillRow(name: String, rank: Proficiency, bonus: Int) {
    val p = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), fontFamily = Cormorant, fontSize = 15.sp, color = p.ink)
        Text(rank.letter, fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 1.sp, color = p.inkMute, modifier = Modifier.padding(end = 12.dp))
        Text(bonus.signed(), fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = p.accent)
    }
}

/** Condition capsule chip (display-only; removal lives in the actions sheet). */
@Composable
private fun PfConditionCapsule(label: String) {
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(50)
    Text(
        label,
        fontFamily = ImFell,
        fontStyle = FontStyle.Italic,
        fontSize = 13.sp,
        color = p.ink,
        modifier = Modifier.clip(shape).background(p.tileStrong).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** A "label: names" line for the spellcasting section. */
@Composable
private fun pfSpellLine(label: String, ids: List<String>) {
    if (ids.isEmpty()) return
    val p = MaterialTheme.natPalette
    val names = ids.mapNotNull { PfSpells.by(it) }.sortedBy { it.name }.joinToString(", ") { it.name }
    Column(Modifier.padding(top = 8.dp)) {
        Text(label.uppercase(), fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.sp, color = p.inkMute)
        Text(names, fontFamily = Cormorant, fontSize = 15.sp, color = p.ink)
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun pfHeroLine(payload: PathfinderPayload): String {
    val ancestry = payload.ancestry.takeIf { it.isNotEmpty() }?.slugToTitle()
    val subclass = payload.subclass?.let { id ->
        PathfinderCatalog.pfClass(payload.className)?.subclasses?.firstOrNull { it.id == id }?.name ?: id.slugToTitle()
    }
    val cls = payload.className.takeIf { it.isNotEmpty() }?.slugToTitle()
    val identity = listOfNotNull(ancestry, subclass, cls).joinToString(" ")
    return (identity.takeIf { it.isNotEmpty() }?.let { "$it · " } ?: "") + "Level ${payload.level}"
}

/** "Wearing {armor+runes} · {shield}" or null when nothing is worn/held. */
private fun pfWearingLine(payload: PathfinderPayload): String? {
    val parts = buildList {
        payload.armor?.let { PfArmors.by(it) }?.let { armor ->
            val potency = payload.armorRunes.potency
            add(armor.name + if (potency > 0) " +$potency" else "")
        }
        payload.shield?.let { PfShields.by(it) }?.let { add(it.name + if (payload.shieldRaised) " (raised)" else "") }
    }
    return if (parts.isEmpty()) null else "Wearing " + parts.joinToString(" · ")
}

/** Map the PF2e action-cost string ("1"/"2"/"3"/"R"/"1 to 3") to its glyph. */
private fun threeActionGlyph(actions: String): String = when (actions.trim()) {
    "1" -> "◆"
    "2" -> "◆◆"
    "3" -> "◆◆◆"
    "R", "r" -> "↺"
    "F" -> "◇"
    else -> "◆–◆◆◆"
}
