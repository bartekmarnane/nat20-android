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
// D&D 5e ruleset engine (pure Kotlin/JVM, depends on :domain)
include(":ruleset-dnd5e")
// Persistence + repository (Android library: Room lives here later)
include(":data")
// The Android app (Compose UI, entry point)
include(":app")
