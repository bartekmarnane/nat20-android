package au.com.evonet.nat20.chronicle

/**
 * The on-device language model behind the journal's chronicle prose. Kept as a
 * narrow seam so [ChronicleService] (and the UI's availability gating) never
 * depend on a concrete model. Mirrors the role of Apple `FoundationModels` on
 * iOS — but where iOS gets a first-party system framework, Android's on-device
 * model (Gemini Nano via AICore / ML Kit GenAI) is an optional, hardware-gated
 * capability, so [isAvailable] is load-bearing here.
 */
interface ChronicleGenerator {
    /**
     * Whether on-device generation is usable right now. This is **the** single
     * on-device-AI availability flag — every chronicle affordance keys off it,
     * and a future "AI character creation" step reuses the same discipline.
     * False ⇒ the affordance simply doesn't appear.
     */
    val isAvailable: Boolean

    /**
     * Generates prose for [userPrompt] under [systemPrompt] (instructions),
     * or null if unavailable or the model declines/fails. Suspending — the
     * caller runs it off the main thread.
     */
    suspend fun generate(systemPrompt: String, userPrompt: String): String?
}
