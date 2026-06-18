package au.com.evonet.nat20.chronicle

import android.content.Context

/**
 * [ChronicleGenerator] backed by **Gemini Nano** (AICore / ML Kit GenAI) — the
 * Android counterpart to iOS's `FoundationModels` chronicler.
 *
 * **Hardware-gated, and currently inert by design.** Gemini Nano runs only on
 * specific devices (Pixel 8+/9 with the AICore system app) and requires the
 * experimental `com.google.ai.edge.aicore` SDK, which can't be exercised in CI
 * or a stock emulator. So until that dependency is added and the app runs on
 * supported hardware, [isAvailable] is false and [generate] returns null — and
 * because every chronicle affordance keys off [isAvailable], the feature simply
 * doesn't appear, exactly the graceful-degradation contract the app uses for
 * all on-device AI. This is the one remaining piece of A7d that needs a real
 * device to finish.
 *
 * Binding the model (when the SDK is added) is contained to [probe]/[generate]:
 * ```
 * // build.gradle: implementation("com.google.ai.edge.aicore:aicore:<ver>")
 * private val model = GenerativeModel(
 *     generationConfig {
 *         context = appContext
 *         temperature = 0.4f; topK = 16; maxOutputTokens = 600
 *     },
 * )
 * // probe(): model.prepareInferenceEngine() succeeds (downloaded + ready)
 * // generate(): model.generateContent("$systemPrompt\n\n$userPrompt").text
 * ```
 */
class GeminiNanoChronicleGenerator(
    @Suppress("unused") private val appContext: Context,
) : ChronicleGenerator {

    override val isAvailable: Boolean = probe()

    /**
     * Returns whether AICore reports Gemini Nano as downloaded and ready.
     * Stubbed to false until the AICore SDK is wired (see class doc).
     */
    private fun probe(): Boolean = false

    override suspend fun generate(systemPrompt: String, userPrompt: String): String? {
        if (!isAvailable) return null
        // model.generateContent("$systemPrompt\n\n$userPrompt").text  (see class doc)
        return null
    }
}
