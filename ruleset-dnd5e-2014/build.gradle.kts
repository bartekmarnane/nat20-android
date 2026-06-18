plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    // Enables `api(...)` so the edition-agnostic core value types that appear in
    // this module's public payload API (e.g. AbilityScores) are visible to
    // consumers (:app) without each one re-declaring :ruleset-dnd5e-core.
    `java-library`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    // Edition-agnostic 5e machinery (abilities, modifier math, proficiency, HP).
    // `api` because these value types leak into this module's public payload API.
    api(project(":ruleset-dnd5e-core"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
