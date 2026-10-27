plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing material is injected by CI through environment variables.
// Nothing secret is stored in the repository.
val signingKeystorePath: String? = System.getenv("IDROP_KEYSTORE_PATH")
    ?.takeIf { it.isNotBlank() }
val signingStorePassword: String? = System.getenv("IDROP_KEYSTORE_PASSWORD")
    ?.takeIf { it.isNotBlank() }
val signingKeyAlias: String? = System.getenv("IDROP_KEY_ALIAS")
    ?.takeIf { it.isNotBlank() }
val signingKeyPassword: String? = System.getenv("IDROP_KEY_PASSWORD")
    ?.takeIf { it.isNotBlank() }
val releaseSigningAvailable: Boolean =
    signingKeystorePath != null &&
        file(signingKeystorePath).isFile &&
        file(signingKeystorePath).length() > 0L &&
        signingStorePassword != null &&
        signingKeyAlias != null &&
        signingKeyPassword != null

android {
    namespace = "com.idrop.x"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.idrop.x"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile = file(signingKeystorePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Nearby 19.5.1 ships Kotlin 2.4 metadata, but exposes a Java API.
        // The app compiles against the stable Java surface; skip the metadata gate.
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.datastore:datastore-preferences:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Free Google Play Services API. No billing, no account.
    implementation("com.google.android.gms:play-services-nearby:19.5.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
