plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android") version "2.51.1"
    id("com.google.gms.google-services") version "4.3.15"
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.google.firebase.crashlytics") version "2.9.5"
    kotlin("plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.mayor.kavi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mayor.kavi"
        minSdk = 30
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    viewBinding {
        enable = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("androidx.navigation:navigation-runtime-ktx:2.8.5")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.6")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Kotlin Coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Libraries for Authentication, Analyzing and Cloud Firestore
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-dynamic-links")
    // Dagger - Hilt Dependency Injector
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    // Datastore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    //Google dependencies
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.play:integrity:1.3.0")
    implementation("com.google.android.gms:play-services-games:23.1.0")
    // Google fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.6")
    // Tensorflow & Support Library
    implementation("org.tensorflow:tensorflow-lite:2.13.0") {
        exclude("org.tensorflow", "tensorflow-lite-api")
    }
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
        exclude("org.tensorflow", "tensorflow-lite-support-api")
    }
    // JSON De/Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    // Documentation library
    implementation("org.jetbrains.dokka:android-documentation-plugin:1.9.20")
    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")
    // Timber logger
    implementation("com.jakewharton.timber:timber:5.0.1")
    // Lottie animation
    implementation("com.airbnb.android:lottie-compose:6.6.1")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    // Vico Charts
    implementation("com.patrykandpatrick.vico:compose:1.12.0")
    implementation("com.patrykandpatrick.vico:compose-m3:1.12.0")
    implementation("com.patrykandpatrick.vico:core:1.12.0")
    // AR and 3D rendering
//    implementation("com.google.ar:core:1.40.0")
//    implementation("com.google.android.filament:filament-android:1.45.0")
//    implementation("com.google.android.filament:filament-utils-android:1.45.0")
//    implementation("com.google.android.filament:gltfio-android:1.45.0")

    // Hilt Test
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
    // Kotlin Coroutines Test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    // Mockito Test
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-inline:5.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation ("io.mockk:mockk:1.13.9")
    testImplementation ("junit:junit:4.13.2")
    testImplementation(kotlin("test"))

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Testing Dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("app.cash.turbine:turbine:1.0.0") // For Flow testing
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.9")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")
}

