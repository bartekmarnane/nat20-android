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

    fun setAppearance(mode: AppearanceMode) {
        prefs.edit().putString(KEY_APPEARANCE, mode.name).apply()
        _appearance.value = mode
    }

    private fun readAppearance(): AppearanceMode =
        prefs.getString(KEY_APPEARANCE, null)
            ?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() }
            ?: AppearanceMode.SYSTEM

    private companion object {
        const val KEY_APPEARANCE = "appearance"
    }
}
