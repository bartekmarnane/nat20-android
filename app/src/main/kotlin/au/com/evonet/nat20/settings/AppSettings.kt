package au.com.evonet.nat20.settings

import android.content.Context
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

    fun setAppearance(mode: AppearanceMode) {
        prefs.edit().putString(KEY_APPEARANCE, mode.name).apply()
        _appearance.value = mode
    }

    fun setOnboardingComplete(value: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()
        _onboardingComplete.value = value
    }

    private fun readAppearance(): AppearanceMode =
        prefs.getString(KEY_APPEARANCE, null)
            ?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() }
            ?: AppearanceMode.SYSTEM

    private companion object {
        const val KEY_APPEARANCE = "appearance"
        const val KEY_ONBOARDING_COMPLETE = "onboarding.completed"
    }
}
