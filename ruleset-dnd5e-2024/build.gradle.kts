plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    // `api(...)` so the shared core value types in the 2024 payload's public API
    // (AbilityScores, ActiveEffect, …) reach consumers (:app) without re-declaring core.
    `java-library`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    // Edition-agnostic 5e machinery shared with the 2014 edition — this is the
    // seam A18 proves out: the 2024 engine reuses the core wholesale.
    api(project(":ruleset-dnd5e-core"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
