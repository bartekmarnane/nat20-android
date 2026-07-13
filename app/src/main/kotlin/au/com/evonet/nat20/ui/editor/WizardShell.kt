package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.DiamondShape
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * Shared creation/edit wizard chrome, the port of iOS `EditorShell`
 * (`CharacterCreationWizard.swift`): back-circle nav row with kicker + title,
 * diamond step indicator, hairline divider, the step content well, and a
 * pinned gradient footer bar. Every ruleset's wizard (and the unified
 * ruleset-choice step) renders inside this one shell; step BODIES keep their
 * own styling — that restyle is a later parity item.
 */
@Composable
fun EditorShell(
    kicker: String,
    title: String,
    stepCount: Int,
    currentIndex: Int,
    onBack: () -> Unit,
    onJump: ((Int) -> Unit)?,
    scrollableContent: Boolean = true,
    footer: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Nav row: back circle · centred kicker/title · balancing spacer.
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, start = 22.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(palette.tileStrong)
                    .border(1.2.dp, palette.ink, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = palette.ink,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    kicker.uppercase(),
                    fontFamily = Cinzel,
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                    color = palette.inkMute,
                )
                Text(
                    title,
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 22.sp,
                    color = palette.ink,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(38.dp))
        }

        WizardStepIndicator(
            count = stepCount,
            currentIndex = currentIndex,
            onJump = onJump,
            modifier = Modifier.padding(top = 14.dp, start = 22.dp, end = 22.dp),
        )

        Box(
            Modifier
                .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 10.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.ink.copy(alpha = 0.4f)),
        )

        if (scrollableContent) {
            val scrollState = rememberScrollState()
            LaunchedEffect(currentIndex) { scrollState.scrollTo(0) }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(start = 22.dp, end = 22.dp, bottom = 16.dp),
                content = content,
            )
        } else {
            // The step area manages its own scrolling (LazyColumn / verticalScroll bodies).
            Column(Modifier.weight(1f).fillMaxWidth(), content = content)
        }

        // Pinned footer bar (same gradient + top-hairline pattern as the roster's CTA bar).
        val hairline = palette.ink.copy(alpha = 0.13f)
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.cream.copy(alpha = 0.5f),
                            palette.cream.copy(alpha = 0.98f),
                            palette.cream.copy(alpha = 0.98f),
                        ),
                    ),
                )
                .drawBehind { drawRect(color = hairline, size = Size(size.width, 1.dp.toPx())) }
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = footer,
        )
    }
}

/**
 * The diamond progress rail: a large active diamond with a cream inset, small
 * solid diamonds for completed steps, stroked ones ahead, hairline connectors
 * between. Visited diamonds are tappable when [onJump] is provided.
 */
@Composable
fun WizardStepIndicator(
    count: Int,
    currentIndex: Int,
    onJump: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.natPalette
    Row(modifier.fillMaxWidth().height(12.dp), verticalAlignment = Alignment.CenterVertically) {
        for (index in 0 until count) {
            if (index > 0) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .height(1.dp)
                        .background(if (index <= currentIndex) palette.accent else palette.ink.copy(alpha = 0.2f)),
                )
            }
            val clickMod = if (onJump != null && index <= currentIndex) {
                Modifier.clickable { onJump(index) }
            } else {
                Modifier
            }
            when {
                index == currentIndex -> Box(
                    Modifier.size(12.dp).clip(DiamondShape).then(clickMod).background(palette.accent),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(2.5.dp)
                            .clip(DiamondShape)
                            .background(palette.cream.copy(alpha = 0.85f)),
                    )
                }
                index < currentIndex -> Box(
                    Modifier.size(7.dp).clip(DiamondShape).then(clickMod).background(palette.accent),
                )
                else -> Box(
                    Modifier.size(7.dp).border(1.dp, palette.ink.copy(alpha = 0.33f), DiamondShape),
                )
            }
        }
    }
}

/** Filled accent capsule CTA — the wizard footer's Continue/Create/Save button. */
@Composable
fun WizardPrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .graphicsLayer(alpha = if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .background(palette.accent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            color = palette.cream,
            maxLines = 1,
        )
    }
}

/** Hollow hairline capsule — the wizard footer's Cancel button. */
@Composable
fun WizardSecondaryButton(label: String, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .clip(CircleShape)
            .border(1.dp, palette.ink.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            color = palette.ink,
            maxLines = 1,
        )
    }
}

/** Ruled step-section header: small-caps accent title, fading rule, optional IM Fell subtitle. */
@Composable
fun WizardStepSection(title: String, subtitle: String? = null) {
    val palette = MaterialTheme.natPalette
    Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title.uppercase(),
                fontFamily = Cinzel,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                color = palette.accent,
                maxLines = 1,
            )
            Box(
                Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(palette.accent.copy(alpha = 0.33f), Color.Transparent))),
            )
        }
        if (subtitle != null) {
            Text(
                subtitle,
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 12.sp,
                color = palette.ink.copy(alpha = 0.67f),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
