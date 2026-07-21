package au.com.evonet.nat20.ui.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.pf2e.ClassProgression
import au.com.evonet.nat20.pf2e.PathfinderPayload
import au.com.evonet.nat20.pf2e.PfLevelUp
import au.com.evonet.nat20.pf2e.core.AdvancementSchedule
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.ui.editor.EditorShell
import au.com.evonet.nat20.ui.editor.ReviewLine
import au.com.evonet.nat20.ui.editor.ReviewSectionHeader
import au.com.evonet.nat20.ui.editor.WizardChipsPicker
import au.com.evonet.nat20.ui.editor.WizardMultiSelectChips
import au.com.evonet.nat20.ui.editor.WizardPrimaryButton
import au.com.evonet.nat20.ui.editor.WizardQuoteBlock
import au.com.evonet.nat20.ui.editor.WizardSecondaryButton
import au.com.evonet.nat20.ui.editor.WizardStepSection
import au.com.evonet.nat20.ui.editor.jsonStateSaver
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/** The Pathfinder 2e level-up wizard's steps (mirrors the 2024 `LevelUp2024Wizard`). */
private enum class PfLuStep(val title: String) {
    BOOSTS("Ability Boosts"),
    SKILL("Skill Increase"),
    REVIEW("Review"),
}

/**
 * The full-screen stepped Pathfinder 2e level-up wizard (parity #36): Ability
 * Boosts (conditional, at levels 5/10/15/20) → Skill Increase (conditional, at
 * odd levels from 3) → Review, hosted in the shared [EditorShell] parchment
 * chrome exactly like the 2014/2024 level-ups. It replaces the old stock-Material
 * [PfLevelUpDialog].
 *
 * PF2e advancement is almost entirely automatic — HP and every proficiency-scaled
 * statistic (perception, saves, class DC, spell/weapon/armor proficiency) improve
 * the moment the level bumps, and any class proficiency-rank jumps ride along.
 * The only *choices* Android's [PfLevelUp] intent carries are the four ability
 * boosts (at boost levels) and the single optional skill increase (at odd levels),
 * so those are the only interactive steps; levels that grant neither collapse to a
 * one-step Review. Fires the existing flat [PfLevelUp] intent unchanged.
 *
 * **Engine gaps deferred (parity #36):** a Monk's Path to Perfection and per-level
 * class/ancestry/skill/general **feat slots** taken at level-up are part of iOS's
 * `PathfinderLevelChoicesBlock` but *not* modelled by Android's [PfLevelUp] intent
 * (the same pattern as the 2014 level-up's deferred choice types). They are taken
 * separately through the actions sheet's Take Feat picker for now.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PfLevelUpWizard(payload: PathfinderPayload, onApplyIntent: (CharacterIntent) -> Unit, onDismiss: () -> Unit) {
    val newLevel = payload.level + 1
    val grantsSkill = AdvancementSchedule.grantsSkillIncrease(newLevel)
    val grantsBoosts = AdvancementSchedule.grantsAbilityBoosts(newLevel)
    val maxRank = AdvancementSchedule.maxSkillRank(newLevel)
    val jumps = ClassProgression.increasesAt(payload.className, newLevel)

    var skill by rememberSaveable(stateSaver = jsonStateSaver<PfSkill?>()) { mutableStateOf(null) }
    var boosts by rememberSaveable(stateSaver = jsonStateSaver<List<PfAbility>>()) { mutableStateOf(emptyList()) }

    // Skills whose current rank can still be raised one step under this level's ceiling.
    val eligibleSkills = remember(newLevel) {
        PfSkill.entries.filter { s ->
            val cur = payload.skills[s] ?: Proficiency.UNTRAINED
            (cur.next?.rank ?: 99) <= maxRank.rank
        }
    }

    val boostsReady = !grantsBoosts || boosts.size == AdvancementSchedule.BOOSTS_PER_ABILITY_BOOST_LEVEL
    val canCommit = payload.level < PathfinderPayload.MAX_LEVEL && boostsReady

    // Active steps — the two conditionals drop out when the level doesn't grant them.
    val active = PfLuStep.entries.filter {
        (it != PfLuStep.BOOSTS || grantsBoosts) && (it != PfLuStep.SKILL || grantsSkill)
    }
    var currentStep by rememberSaveable { mutableStateOf(active.first()) }
    var furthest by rememberSaveable { mutableStateOf(active.first()) }
    if (currentStep !in active) currentStep = active.first()
    val position = active.indexOf(currentStep).coerceAtLeast(0)

    fun canAdvance(): Boolean = when (currentStep) {
        PfLuStep.BOOSTS -> true // lenient — the Review's Level-up button is the real gate
        PfLuStep.SKILL -> true // the skill increase is optional
        PfLuStep.REVIEW -> canCommit
    }

    fun advance() {
        val next = active.getOrNull(position + 1) ?: return
        currentStep = next
        if (next.ordinal > furthest.ordinal) furthest = next
    }

    fun back() {
        val prev = active.getOrNull(position - 1)
        if (prev == null) onDismiss() else currentStep = prev
    }

    fun commit() {
        onApplyIntent(
            PfLevelUp(
                skillIncrease = skill.takeIf { grantsSkill },
                abilityBoosts = if (grantsBoosts) boosts else emptyList(),
            ),
        )
        onDismiss()
    }

    val palette = MaterialTheme.natPalette
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(palette.parchment)) {
            EditorShell(
                kicker = "Level up · Pathfinder · Step ${position + 1} of ${active.size}",
                title = currentStep.title,
                stepCount = active.size,
                currentIndex = position,
                onBack = { back() },
                onJump = { target ->
                    active.getOrNull(target)?.let { step ->
                        if (step.ordinal <= furthest.ordinal) currentStep = step
                    }
                },
                footer = {
                    WizardSecondaryButton("Cancel", onDismiss)
                    Spacer(Modifier.weight(1f))
                    if (currentStep == PfLuStep.REVIEW) {
                        WizardPrimaryButton("Level up", enabled = canCommit) { commit() }
                    } else {
                        WizardPrimaryButton("Continue", enabled = canAdvance()) { advance() }
                    }
                },
            ) {
                when (currentStep) {
                    // ── Ability Boosts (levels 5/10/15/20) ──
                    PfLuStep.BOOSTS -> {
                        WizardStepSection(
                            "Four ability boosts",
                            subtitle = "Each raises a score by +2 (only +1 once it's 18 or higher). No ability twice.",
                        )
                        WizardMultiSelectChips(
                            PfAbility.entries.toList(),
                            isSelected = { it in boosts },
                            label = { "${it.abbreviation} ${payload.abilityScores.score(it)}" },
                            quotaFull = boosts.size >= AdvancementSchedule.BOOSTS_PER_ABILITY_BOOST_LEVEL,
                        ) { ability ->
                            boosts = if (ability in boosts) boosts - ability else boosts + ability
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${boosts.size}/${AdvancementSchedule.BOOSTS_PER_ABILITY_BOOST_LEVEL} chosen",
                            fontFamily = Cinzel,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = if (boostsReady) palette.accent else palette.inkMute,
                        )
                    }

                    // ── Skill Increase (odd levels from 3) ──
                    PfLuStep.SKILL -> {
                        WizardStepSection(
                            "Raise one skill",
                            subtitle = "Increase a skill one rank, up to ${maxRank.displayName} at level $newLevel. Optional.",
                        )
                        if (eligibleSkills.isEmpty()) {
                            Text(
                                "No skills can be raised further at this level.",
                                fontFamily = ImFell,
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp,
                                color = palette.inkMute,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        } else {
                            WizardChipsPicker(
                                eligibleSkills,
                                isSelected = { it == skill },
                                label = { s ->
                                    val cur = payload.skills[s] ?: Proficiency.UNTRAINED
                                    "${s.displayName} ${cur.letter}→${cur.next?.letter ?: "—"}"
                                },
                            ) { picked -> skill = if (skill == picked) null else picked }
                        }
                    }

                    // ── Review ──
                    PfLuStep.REVIEW -> {
                        ReviewSectionHeader("Confirm")
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.tile)
                                .border(1.dp, palette.ink.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(10.dp),
                        ) {
                            ReviewLine("Level", "${payload.level} → $newLevel")
                            ReviewLine("Automatic", "HP and every proficiency bonus improve")
                            if (jumps.isNotEmpty()) {
                                ReviewLine(
                                    "Class advances",
                                    jumps.joinToString(", ") {
                                        "${it.track.name.lowercase().replaceFirstChar(Char::uppercase)} → ${it.rank.displayName}"
                                    },
                                )
                            }
                            if (grantsSkill) {
                                val s = skill
                                ReviewLine(
                                    "Skill increase",
                                    if (s == null) {
                                        "None"
                                    } else {
                                        val cur = payload.skills[s] ?: Proficiency.UNTRAINED
                                        "${s.displayName} → ${cur.next?.displayName ?: cur.displayName}"
                                    },
                                )
                            }
                            if (grantsBoosts) {
                                ReviewLine(
                                    "Ability boosts",
                                    if (boosts.isEmpty()) "—" else boosts.joinToString(" · ") { it.abbreviation },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        WizardQuoteBlock(
                            null,
                            "In Pathfinder 2e your level itself scales every bonus, so most of a level-up happens automatically — you only pick the choices above.",
                        )
                    }
                }
            }
        }
    }
}
