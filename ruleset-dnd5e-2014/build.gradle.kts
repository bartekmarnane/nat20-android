plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    // Edition-agnostic 5e machinery (abilities, modifier math, proficiency, HP)
    implementation(project(":ruleset-dnd5e-core"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
