plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

// Edition-agnostic D&D 5e machinery shared by every 5e edition package
// (2014 now, 2024 later). Mirrors the iOS `DnD5eCore` package. Pure
// Kotlin/JVM — no Android deps — so it stays host-unit-testable.
dependencies {
    // :domain is the architectural arrow (core composes domain value types as
    // it grows — effects, etc.); the current A3 slice is pure math/value types.
    implementation(project(":domain"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
