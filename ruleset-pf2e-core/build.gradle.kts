plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

// PF2e maths shared by the Pathfinder package: the five-rank proficiency ladder,
// four degrees of success, abilities/saves/skills, traditions, valued conditions.
// Mirrors the iOS `PathfinderCore` package. Pure Kotlin/JVM — no Android deps.
dependencies {
    implementation(project(":domain"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
