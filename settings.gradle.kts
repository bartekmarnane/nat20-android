pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Lets Gradle auto-download a matching JDK when a module requests a toolchain
// (e.g. the pure-Kotlin :domain / :ruleset-dnd5e modules ask for JDK 17).
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Nat20"

// Ruleset-agnostic core (pure Kotlin/JVM — no Android deps)
include(":domain")
// Edition-agnostic 5e machinery shared by every 5e edition (mirrors iOS DnD5eCore)
include(":ruleset-dnd5e-core")
// D&D 5e (2014) engine (pure Kotlin/JVM, depends on :ruleset-dnd5e-core)
include(":ruleset-dnd5e-2014")
// D&D 5e (2024) engine — sibling edition on the shared core (mirrors iOS DnD5e2024)
include(":ruleset-dnd5e-2024")
// Pathfinder 2e (Remaster) maths core (mirrors iOS PathfinderCore) — first non-D&D system
include(":ruleset-pf2e-core")
// Pathfinder 2e (Remaster) engine on its own core (mirrors iOS Pathfinder)
include(":ruleset-pf2e")
// Persistence + repository (Android library: Room lives here later)
include(":data")
// The Android app (Compose UI, entry point)
include(":app")
