package au.com.evonet.nat20.settings

import android.content.Context
import au.com.evonet.nat20.chronicle.NarrationStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Light / dark / follow-system appearance choice. */
enum class AppearanceMode { SYSTEM, LIGHT, DARK }

/**
 * Lightweight persisted app settings (A9). Backed by `SharedPreferences` (no
 * extra dependency); exposes a [StateFlow] so the theme re-reads reactively.
 * The iOS counterpart is `AppSettings` over `UserDefaults`.
 */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("nat20.settings", Context.MODE_PRIVATE)

    private val _appearance = MutableStateFlow(readAppearance())
    val appearance: StateFlow<AppearanceMode> = _appearance.asStateFlow()

    /** Whether the first-run onboarding flow has been completed (A12). */
    private val _onboardingComplete = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    /** The journal narration style — Simple (plain) or Storied (AI prose) (A9). */
    private val _narrationStyle = MutableStateFlow(readNarrationStyle())
    val narrationStyle: StateFlow<NarrationStyle> = _narrationStyle.asStateFlow()

    /** Debug-only: replay the first-run onboarding flow on every launch (iOS `alwaysShowOnboarding`). */
    private val _alwaysShowOnboarding = MutableStateFlow(prefs.getBoolean(KEY_ALWAYS_SHOW_ONBOARDING, false))
    val alwaysShowOnboarding: StateFlow<Boolean> = _alwaysShowOnboarding.asStateFlow()

    fun setAppearance(mode: AppearanceMode) {
        prefs.edit().putString(KEY_APPEARANCE, mode.name).apply()
        _appearance.value = mode
    }

    fun setNarrationStyle(style: NarrationStyle) {
        prefs.edit().putString(KEY_NARRATION_STYLE, style.name).apply()
        _narrationStyle.value = style
    }

    fun setOnboardingComplete(value: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()
        _onboardingComplete.value = value
    }

    fun setAlwaysShowOnboarding(value: Boolean) {
        prefs.edit().putBoolean(KEY_ALWAYS_SHOW_ONBOARDING, value).apply()
        _alwaysShowOnboarding.value = value
    }

    private fun readAppearance(): AppearanceMode =
        prefs.getString(KEY_APPEARANCE, null)
            ?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() }
            ?: AppearanceMode.SYSTEM

    private fun readNarrationStyle(): NarrationStyle =
        prefs.getString(KEY_NARRATION_STYLE, null)
            ?.let { runCatching { NarrationStyle.valueOf(it) }.getOrNull() }
            ?: NarrationStyle.STORIED

    private companion object {
        const val KEY_APPEARANCE = "appearance"
        const val KEY_ONBOARDING_COMPLETE = "onboarding.completed"
        const val KEY_NARRATION_STYLE = "narration.style"
        const val KEY_ALWAYS_SHOW_ONBOARDING = "onboarding.alwaysShow"
    }
}
