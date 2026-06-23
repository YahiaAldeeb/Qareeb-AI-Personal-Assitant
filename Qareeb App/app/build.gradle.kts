plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.qareeb"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.qareeb"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
        viewBinding = true
    }
}

dependencies {

    /* ---------------- Core Android ---------------- */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")  // ✅ for FragmentActivity/AppCompatActivity

    /* ---------------- Activity ---------------- */
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(libs.firebase.messaging.ktx)

    /* ---------------- Compose (BOM) ---------------- */
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.geometry)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    /* ---------------- Navigation ---------------- */
    implementation("androidx.navigation:navigation-compose:2.7.7")

    /* ---------------- Security ---------------- */
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")  // ✅ for BiometricPrompt

    /* ---------------- Database (Room) ---------------- */
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    /* ---------------- Networking ---------------- */
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    /* ---------------- Coroutines ---------------- */
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    /* ---------------- Lifecycle ---------------- */
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")  // ✅ for ProcessLifecycleOwner

    /* ---------------- WorkManager ---------------- */
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    /* ---------------- AI / Animations ---------------- */
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("com.alphacephei:vosk-android:0.3.47")

    /* ---------------- Testing ---------------- */
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.google.firebase:firebase-messaging-ktx:23.4.1")
    // Import the Firebase BOM — it manages all Firebase versions for you
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // No version number needed when using BOM
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Add any other Firebase libs here without versions too
    // implementation("com.google.firebase:firebase-analytics-ktx")
}
