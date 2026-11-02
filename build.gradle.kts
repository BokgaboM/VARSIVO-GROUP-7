plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.varsivo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.varsivo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // XML layouts + View Binding (no Jetpack Compose anywhere in this project).
    buildFeatures {
        viewBinding = true
        buildConfig = true // BuildConfig.VERSION_NAME is shown on the Settings screen
    }

    testOptions {
        unitTests {
            // Lets plain JVM unit tests call android.util.Log without crashing.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Android Views, Material components and navigation
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // REST API client (Retrofit + Gson) and the map SDK (MapLibre, OpenStreetMap based)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.maplibre.android)

    // Local unit tests (run on the JVM, also in GitHub Actions)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests (need an emulator / device)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
