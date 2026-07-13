package au.com.evonet.nat20.ui.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.CastSpell
import au.com.evonet.nat20.dnd5e.CreatureCatalog
import au.com.evonet.nat20.dnd5e.CreatureTemplate
import au.com.evonet.nat20.dnd5e.SummonCreature
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.GameDuration
import au.com.evonet.nat20.domain.Summon
import au.com.evonet.nat20.domain.SummonLifecycle
import au.com.evonet.nat20.domain.SummonOrigin
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette
import java.time.Instant

/**
 * The slice-C SUMMON pickers (parity #19): Find Familiar / Find Steed / Beast
 * Companion (persistent) and Conjure Animals / Animate Dead (slot-consuming
 * casts) plus the Tasha's summon router. Each drives the hand-authored
 * [CreatureCatalog] roster (Android's smaller stand-in for iOS's 330-monster
 * `MonsterCatalog` — documented deviation) and emits the same `CastSpell` +
 * `SummonCreature` combos the iOS pickers do. All commit through the layer's
 * `onApplyIntent`, so summoning journals in a campaign.
 *
 * Pickers return an ordered intent list; the layer applies each in turn
 * (cast-then-summon, matching iOS where the caller fires `CastSpell` first to
 * drain the slot + set concentration, then `SummonCreature`).
 */

private fun ordinal(n: Int): String = when (n) {
    1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${n}th"
}

// ── Find Familiar ────────────────────────────────────────────────────────────

@Composable
internal fun FindFamiliarPicker(onCancel: () -> Unit, onCommit: (List<CharacterIntent>) -> Unit) {
    SpeciesSummonPicker(
        kicker = "Magic",
        title = "Find Familiar",
        speciesLabel = "Species",
        templates = CreatureCatalog.familiars,
        defaultName = "Familiar",
        commitLabel = "Summon",
        onCancel = onCancel,
    ) { template, name ->
        val summon = CreatureCatalog.buildSummon(
            template = template,
            origin = SummonOrigin.Spell("Find Familiar", 1),
            lifecycle = SummonLifecycle.Persistent,
            spawnedAt = Instant.now(),
            customName = name,
            label = if (name.isBlank()) "Find Familiar — ${template.name}" else "Find Familiar — $name (${template.name})",
        )
        onCommit(listOf(SummonCreature(summon)))
    }
}

// ── Find Steed ───────────────────────────────────────────────────────────────

@Composable
internal fun FindSteedPicker(onCancel: () -> Unit, onCommit: (List<CharacterIntent>) -> Unit) {
    SpeciesSummonPicker(
        kicker = "Magic",
        title = "Find Steed",
        speciesLabel = "Mount",
        templates = CreatureCatalog.mounts,
        defaultName = "Steed",
        commitLabel = "Summon",
        onCancel = onCancel,
    ) { template, name ->
        val summon = CreatureCatalog.buildSummon(
            template = template,
            origin = SummonOrigin.Spell("Find Steed", 2),
            lifecycle = SummonLifecycle.Persistent,
            spawnedAt = Instant.now(),
            customName = name,
            label = if (name.isBlank()) "Find Steed — ${template.name}" else "Find Steed — $name (${template.name})",
        )
        onCommit(listOf(SummonCreature(summon)))
    }
}

// ── Beast Master companion ───────────────────────────────────────────────────

/**
 * Beast Master's L3 Ranger's Companion. Per PHB the companion's max HP is
 * `max(beast HP, 4 × ranger level)`; the boost is baked into the spawned
 * creature at bind time (later level-ups don't auto-propagate — dismiss and
 * re-bind, matching iOS).
 */
@Composable
internal fun BeastCompanionPicker(rangerLevel: Int, onCancel: () -> Unit, onCommit: (List<CharacterIntent>) -> Unit) {
    SpeciesSummonPicker(
        kicker = "Resources",
        title = "Bind Companion",
        speciesLabel = "Beast",
        templates = CreatureCatalog.companions.filter { it.kind == au.com.evonet.nat20.dnd5e.CreatureKind.COMPANION },
        defaultName = "Companion",
        commitLabel = "Bind",
        hpBoost = { maxOf(it.maxHp, 4 * rangerLevel) },
        onCancel = onCancel,
    ) { template, name ->
        val boosted = maxOf(template.maxHp, 4 * rangerLevel)
        val creature = template.makeCreature(name.ifBlank { null }).copy(maxHp = boosted, currentHp = boosted)
        val summon = Summon(
            label = if (name.isBlank()) "Beast Companion — ${template.name}" else "Beast Companion — $name (${template.name})",
            origin = SummonOrigin.ClassFeature("Ranger", "Ranger's Companion"),
            lifecycle = SummonLifecycle.Persistent,
            creatures = listOf(creature),
            spawnedAt = Instant.now(),
        )
        onCommit(listOf(SummonCreature(summon)))
    }
}

// ── Conjure Animals ──────────────────────────────────────────────────────────

/** PHB Conjure Animals tiers: fewer-tougher ↔ more-weaker, doubling every 2 slot levels above 3rd. */
private enum class ConjureTier(val baseCount: Int, val label: String, val blurb: String) {
    TWO(1, "1× CR 2", "stronger, fewer"),
    ONE(2, "2× CR 1", "balanced"),
    HALF(4, "4× CR ½", "swarm"),
    QUARTER(8, "8× CR ¼", "horde");

    fun count(slot: Int): Int = baseCount shl maxOf(0, (slot - 3) / 2)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConjureAnimalsPicker(availableSlots: List<Int>, onCancel: () -> Unit, onCommit: (List<CharacterIntent>) -> Unit) {
    val palette = MaterialTheme.natPalette
    val slots = availableSlots.filter { it in 3..9 }.ifEmpty { (3..9).toList() }
    val species = remember { CreatureCatalog.summons }
    var slot by remember { mutableIntStateOf(slots.first()) }
    var tier by remember { mutableStateOf(ConjureTier.QUARTER) }
    var templateId by remember { mutableStateOf(species.first().id) }
    val template = species.first { it.id == templateId }
    val count = tier.count(slot)

    ActionPickerShell(
        kicker = "Magic",
        title = "Conjure Animals",
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(label = "Cast") {
                val summon = CreatureCatalog.buildSummon(
                    template = template,
                    origin = SummonOrigin.Spell("Conjure Animals", slot),
                    lifecycle = SummonLifecycle.Concentration(GameDuration.Hours(1)),
                    spawnedAt = Instant.now(),
                    count = count,
                    label = "Conjure Animals (${ordinal(slot)}) — ${template.name}${if (count > 1) "s" else ""}",
                )
                onCommit(
                    listOf(
                        CastSpell("conjure-animals", "Conjure Animals", spellLevel = 3, slotLevel = slot, requiresConcentration = true),
                        SummonCreature(summon),
                    ),
                )
            }
        },
    ) {
        PickerSection("Slot Level", top = 6.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            slots.forEach { s -> SmallCapsChip("L$s", active = slot == s) { slot = s } }
        }

        PickerSection("Group")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ConjureTier.entries.forEach { t ->
                val active = tier == t
                PickerRow(borderTone = if (active) palette.accent else null, onClick = { tier = t }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${t.count(slot)}× ${t.label.substringAfter("× ")}",
                            fontFamily = Cinzel,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            letterSpacing = 1.5.sp,
                            color = if (active) palette.accent else palette.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Text(t.blurb, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkSoft)
                    }
                }
            }
        }

        PickerSection("Species")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            species.forEach { t -> PickerChip(t.name, active = templateId == t.id) { templateId = t.id } }
        }

        PickerSection("Preview")
        CreaturePreview(template, headline = "$count× ${template.name}")
        PickerGap(8.dp)
    }
}

// ── Animate Dead ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnimateDeadPicker(availableSlots: List<Int>, onCancel: () -> Unit, onCommit: (List<CharacterIntent>) -> Unit) {
    val undead = remember { CreatureCatalog.undead }
    val slots = availableSlots.filter { it in 3..9 }.ifEmpty { (3..9).toList() }
    var slot by remember { mutableIntStateOf(slots.first()) }
    var templateId by remember { mutableStateOf(undead.first().id) }
    val template = undead.first { it.id == templateId }
    val count = 1 + 2 * maxOf(0, slot - 3)

    ActionPickerShell(
        kicker = "Magic",
        title = "Animate Dead",
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(label = "Cast") {
                val summon = CreatureCatalog.buildSummon(
                    template = template,
                    origin = SummonOrigin.Spell("Animate Dead", slot),
                    lifecycle = SummonLifecycle.ControlReassertion(au.com.evonet.nat20.domain.Cadence.DAILY, maxOf(count, 4)),
                    spawnedAt = Instant.now(),
                    count = count,
                    label = "Animate Dead (${ordinal(slot)}) — ${template.name}${if (count > 1) "s" else ""}",
                )
                // Animate Dead is not a concentration spell — existing summons survive.
                onCommit(
                    listOf(
                        CastSpell("animate-dead", "Animate Dead", spellLevel = 3, slotLevel = slot),
                        SummonCreature(summon),
                    ),
                )
            }
        },
    ) {
        PickerSection("Slot Level", top = 6.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            slots.forEach { s -> SmallCapsChip("L$s", active = slot == s) { slot = s } }
        }

        PickerSection("Undead Kind")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            undead.forEach { t -> SmallCapsChip(t.name, active = templateId == t.id) { templateId = t.id } }
        }

        PickerSection("Preview")
        CreaturePreview(template, headline = "$count× ${template.name}")
        PickerHelpText("Reassert control on the 24-hour anniversary or the undead turn hostile.", Modifier.padding(top = 8.dp))
        PickerGap(8.dp)
    }
}

// ── Tasha's summon router (deferred catalogue) ───────────────────────────────

/**
 * Router for Tasha's *Summon X* family (iOS `TashasSummonRouter`). Tasha's
 * statblocks aren't openly licensed and Android hasn't ported the
 * `TashasSummonCatalog` (mirrors the iOS A18 deferral), and no `summon-*` spell
 * ships in Android's SRD 5.1 content — so this surfaces the router's empty
 * state. The per-spell picker + stat catalogue land with the licensed content.
 */
@Composable
internal fun TashasSummonRouter(onCancel: () -> Unit) {
    ActionPickerShell(kicker = "Magic", title = "Summon Spirit", onCancel = onCancel) {
        PickerSection("Available", top = 6.dp)
        PickerHelpText(
            "Tasha's summon spells aren't in the bundled SRD content yet. When the licensed statblocks land, " +
                "known summon spells will appear here to cast.",
        )
        PickerGap(8.dp)
    }
}

// ── Shared species picker + preview ──────────────────────────────────────────

/**
 * The shared persistent-summon flow (Find Familiar / Steed / Companion): species
 * chips → optional name → preview → single [SummonCreature]. [onSummon] builds
 * and dispatches the intent list so each caller controls the origin/lifecycle.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeciesSummonPicker(
    kicker: String,
    title: String,
    speciesLabel: String,
    templates: List<CreatureTemplate>,
    defaultName: String,
    commitLabel: String,
    hpBoost: ((CreatureTemplate) -> Int)? = null,
    onCancel: () -> Unit,
    onSummon: (CreatureTemplate, String) -> Unit,
) {
    var templateId by remember { mutableStateOf(templates.firstOrNull()?.id) }
    var name by remember { mutableStateOf("") }
    val template = templates.firstOrNull { it.id == templateId }

    ActionPickerShell(
        kicker = kicker,
        title = title,
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(label = commitLabel, isDisabled = template == null) {
                template?.let { onSummon(it, name.trim()) }
            }
        },
    ) {
        PickerSection(speciesLabel, top = 6.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            templates.forEach { t -> SmallCapsChip(t.name, active = templateId == t.id) { templateId = t.id } }
        }

        template?.let { t ->
            PickerSection("Preview")
            CreaturePreview(t, hpOverride = hpBoost?.invoke(t))
        }

        PickerSection("Name")
        WizardTextField(template?.name ?: defaultName, name, { if (it.length <= 40) name = it })
        PickerGap(8.dp)
    }
}

/** Compact statblock tile for a [CreatureTemplate]. */
@Composable
private fun CreaturePreview(template: CreatureTemplate, headline: String? = null, hpOverride: Int? = null) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(8.dp)
    val hp = hpOverride ?: template.maxHp
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.15f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            headline ?: template.name,
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = palette.ink,
        )
        Text(
            "${template.size} ${template.type}  ·  HP $hp  ·  AC ${template.armorClass}  ·  ${template.speed}",
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 11.sp,
            color = palette.inkMute,
        )
        if (hpOverride != null && hpOverride > template.maxHp) {
            Text(
                "HP boosted from ${template.maxHp} → $hpOverride (Beast Master: 4 × ranger level).",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = palette.accent,
            )
        }
        if (template.attacks.isNotEmpty()) {
            Text(
                template.attacks.joinToString("\n") { it.display },
                fontFamily = EbGaramond,
                fontSize = 12.sp,
                color = palette.inkSoft,
            )
        }
    }
}
