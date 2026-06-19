plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    // `api(...)` so the PF2e core value types in the payload's public API
    // (AbilityScores, Proficiency, …) reach :app without re-declaring the core.
    `java-library`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    api(project(":ruleset-pf2e-core"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
