plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// 加载签名配置
val keystoreProps: Map<String, String> = rootProject.file("keystore.properties").let { f ->
    if (f.exists()) f.readLines().filter { it.contains("=") }.associate {
        val parts = it.split("=", limit = 2)
        parts[0].trim() to parts.getOrElse(1) { "" }.trim()
    } else emptyMap()
}

android {
    namespace = "com.fuelapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fuelapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "1.6.0"
    }

    // ---------- 签名配置 ----------
    signingConfigs {
        create("release") {
            storeFile = rootProject.file(keystoreProps.getOrDefault("storeFile", "fuel-tracker.jks"))
            storePassword = keystoreProps.getOrDefault("storePassword", "")
            keyAlias = keystoreProps.getOrDefault("keyAlias", "fuel-tracker")
            keyPassword = keystoreProps.getOrDefault("keyPassword", "")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // AAB 配置（需要 AGP 8+）
    @Suppress("UnstableApiUsage")
    bundle {
        language {
            enableSplit = false  // 不分语言包，减小总包体积
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true   // 按 CPU 架构分包
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
