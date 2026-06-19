plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "au.com.evonet.nat20"
    compileSdk = 35

    defaultConfig {
        applicationId = "au.com.evonet.nat20"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        // For BuildConfig.DEBUG — gates the first-launch demo seed (A5).
        buildConfig = true
    }
}

dependencies {
    implementation(project(":domain"))
    // The 2014 edition; pulls in :ruleset-dnd5e-core transitively. Additional
    // editions (2024, PF2e) get added here as their modules land.
    implementation(project(":ruleset-dnd5e-2014"))
    implementation(project(":ruleset-dnd5e-2024"))
    // Pathfinder 2e (Remaster) — the first non-D&D ruleset (A22); pulls in :ruleset-pf2e-core.
    implementation(project(":ruleset-pf2e"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
}
