package au.com.evonet.nat20.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.slugToTitle
import java.io.File

/**
 * Exports a character as a printable single-page PDF and hands it to the system
 * share sheet via FileProvider (parity #40's Export action). The layout is a
 * clean front page — identity, ability block, and core vitals — drawn with
 * [PdfDocument]. iOS renders richer multi-page per-ruleset sheets
 * (`CharacterSheet*PDFExporter`); matching that page-for-page is a follow-up.
 */
object CharacterSheetPdf {

    private const val PAGE_W = 595 // A4 @ 72dpi
    private const val PAGE_H = 842

    fun exportAndShare(context: Context, character: Character) {
        val file = render(context, character)
        val uri = FileProvider.getUriForFile(context, "au.com.evonet.nat20.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share character sheet").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun render(context: Context, character: Character): File {
        val summary = summarize(character)
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val canvas = page.canvas
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0x2A, 0x1A, 0x08) }
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0x7A, 0x3B, 0x1D) }
        val serif = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        val serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val serifItalic = Typeface.create(Typeface.SERIF, Typeface.ITALIC)

        fun text(s: String, x: Float, y: Float, size: Float, paint: Paint, face: Typeface, align: Paint.Align = Paint.Align.LEFT) {
            paint.textSize = size; paint.typeface = face; paint.textAlign = align
            canvas.drawText(s, x, y, paint)
        }

        val cx = PAGE_W / 2f
        text(summary.name, cx, 90f, 30f, accent, serifBold, Paint.Align.CENTER)
        text(summary.subtitle, cx, 116f, 14f, ink, serifItalic, Paint.Align.CENTER)
        accent.strokeWidth = 1f
        canvas.drawLine(60f, 140f, PAGE_W - 60f, 140f, accent)

        var y = 180f
        if (summary.abilities.isNotEmpty()) {
            text("ABILITIES", 60f, y, 12f, accent, serifBold); y += 24f
            summary.abilities.chunked(3).forEach { row ->
                var x = 60f
                row.forEach { (abbrev, score, mod) ->
                    text(abbrev, x, y, 11f, ink, serifBold)
                    text("$score  (${if (mod >= 0) "+$mod" else "$mod"})", x + 44f, y, 13f, ink, serif)
                    x += 170f
                }
                y += 26f
            }
            y += 12f
        }
        if (summary.vitals.isNotEmpty()) {
            text("VITALS", 60f, y, 12f, accent, serifBold); y += 24f
            summary.vitals.forEach { (label, value) ->
                text(label, 60f, y, 12f, ink, serifBold)
                text(value, 200f, y, 13f, ink, serif)
                y += 24f
            }
        }
        text("Exported from Nat20", cx, (PAGE_H - 40).toFloat(), 10f, ink, serifItalic, Paint.Align.CENTER)

        doc.finishPage(page)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "${character.name.replace(Regex("[^A-Za-z0-9]"), "_")}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private data class Summary(
        val name: String,
        val subtitle: String,
        val abilities: List<Triple<String, Int, Int>>,
        val vitals: List<Pair<String, String>>,
    )

    private fun summarize(character: Character): Summary = when (val p = character.payload) {
        is DnD5ePayload -> Summary(
            name = character.name,
            subtitle = listOfNotNull(p.race.takeIf { it.isNotEmpty() }?.slugToTitle(), p.characterClass.takeIf { it.isNotEmpty() }?.slugToTitle()?.let { "$it ${p.level}" } ?: "Level ${p.level}").joinToString(" · "),
            abilities = abilityRows(p.abilityScores),
            vitals = listOf("Max HP" to p.maxHp.toString(), "Armor Class" to p.armorClass.toString()),
        )
        is DnD5e2024Payload -> Summary(
            name = character.name,
            subtitle = listOfNotNull(p.species.takeIf { it.isNotEmpty() }?.slugToTitle(), p.characterClass.takeIf { it.isNotEmpty() }?.slugToTitle()?.let { "$it ${p.level}" } ?: "Level ${p.level}").joinToString(" · "),
            abilities = abilityRows(p.abilityScores),
            vitals = listOf("Max HP" to p.maxHp.toString()),
        )
        else -> Summary(
            name = character.name,
            subtitle = character.rulesetId.slugToTitle(),
            abilities = emptyList(),
            vitals = emptyList(),
        )
    }

    private fun abilityRows(scores: AbilityScores): List<Triple<String, Int, Int>> =
        Ability.entries.map { a -> Triple(a.abbreviation, scores.score(a), scores.modifier(a)) }
}
