package au.com.evonet.nat20.app

import android.app.Application
import au.com.evonet.nat20.BuildConfig
import au.com.evonet.nat20.store.DemoCharacters
import kotlinx.coroutines.launch

/**
 * Application entry point. Owns the [AppContainer] and seeds the debug-only
 * demo roster on first launch. Release builds start empty and reach character
 * creation through onboarding (A12) — matching the iOS `#if DEBUG` seed.
 */
class Nat20Application : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Hydrate the homebrew race library from Room, then keep it persisted
        // write-through (parity #11).
        container.customRaceSync.start()

        if (BuildConfig.DEBUG) {
            container.applicationScope.launch {
                container.characterRepository.seedIfEmpty(DemoCharacters.seed())
            }
        }
    }
}
