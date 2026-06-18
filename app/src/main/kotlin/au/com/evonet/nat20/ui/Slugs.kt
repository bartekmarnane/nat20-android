package au.com.evonet.nat20.ui

/**
 * Prettifies a catalogue id slug (`"mountain-dwarf"` → `"Mountain Dwarf"`) for
 * display. A4 stopgap: race/class are stored as slugs and the catalogues that
 * carry real display names aren't loaded until the content steps (A10), so we
 * title-case the slug in the meantime.
 */
fun String.slugToTitle(): String =
    split('-', '_')
        .filter { it.isNotEmpty() }
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
