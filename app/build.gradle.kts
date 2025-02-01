import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android") version "2.51.1"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.jetbrains.dokka") version "1.9.20"
//    id("com.google.firebase.crashlytics") version "3.0.2" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    id("com.google.gms.google-services") version "4.4.2"
}

android {
    namespace = "com.mayor.kavi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mayor.kavi"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Load keystore properties
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (!keystorePropertiesFile.exists()) {
        throw GradleException("Keystore properties file not found at: ${keystorePropertiesFile.absolutePath}")
    }
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))

    // Verify required properties
    val requiredProps = listOf("KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
    val missingProps = requiredProps.filter { !keystoreProperties.containsKey(it) }
    if (missingProps.isNotEmpty()) {
        throw GradleException("Missing required properties in keystore.properties: $missingProps")
    }

    // Release configurations
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("app/keystore/release.keystore")
            storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD")
            keyAlias = keystoreProperties.getProperty("KEY_ALIAS")
            keyPassword = keystoreProperties.getProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            // Read from local.properties
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localProperties.load(FileInputStream(localPropertiesFile))
            }
            val apiKey = localProperties.getProperty("ROBOFLOW_API_KEY", "")
            buildConfigField(
                "String",
                "ROBOFLOW_API_KEY",
                "\"$apiKey\""
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            // Read from local.properties
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localProperties.load(FileInputStream(localPropertiesFile))
            }
            val apiKey = localProperties.getProperty("ROBOFLOW_API_KEY", "")
            buildConfigField(
                "String",
                "ROBOFLOW_API_KEY",
                "\"$apiKey\""
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
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/DEPENDENCIES",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/plexus/components.xml",
                "META-INF/services/javax.annotation.processing.Processor",
                "META-INF/*.version",
                "META-INF/*.kotlin_module"
            )
            pickFirsts += setOf(
                // If there are still conflicts, pick the first occurrence
                "META-INF/LICENSE.md", "META-INF/LICENSE-notice.md", "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt", "META-INF/DEPENDENCIES"
            )
            merges += setOf(
                // Merge similar files
                "META-INF/services/*",
                "META-INF/spring.*"
            )
        }
    }
    viewBinding {
        enable = true
    }

    // Add size optimization configurations
    configurations.all {
        exclude(group = "com.google.android.gms", module = "play-services-games")
        exclude(group = "com.google.android.play", module = "integrity")
    }
    lint {
        disable += "NotificationPermission"
    }
    buildToolsVersion = "35.0.1"
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
    // Firebase SDK
//    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
//    implementation("com.google.firebase:firebase-storage")
//    implementation("com.google.firebase:firebase-auth-ktx")
//    implementation("com.google.firebase:firebase-firestore")
//    implementation("com.google.firebase:firebase-analytics")
//    implementation("com.google.firebase:firebase-crashlytics")
    // Dagger - Hilt Dependency Injector
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    // Datastore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Google dependencies
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.play:integrity:1.3.0")
    implementation("com.google.android.gms:play-services-games:23.1.0")
    // Google fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.6")
    // Tensorflow & Support Library
    implementation("org.tensorflow:tensorflow-lite:2.14.0") {
        exclude("org.tensorflow", "tensorflow-lite-api")
    }
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
        exclude("org.tensorflow", "tensorflow-lite-support-api")
    }
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4") {
        exclude("org.tensorflow", "tensorflow-lite-metadata-api")
    }
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.9.0")
    implementation("io.github.cdimascio:dotenv-java:3.1.0")
    // Camera-X Dependencies
    api("androidx.camera:camera-core:1.4.0")
    api("androidx.camera:camera-camera2:1.4.0")
    api("androidx.camera:camera-lifecycle:1.4.0")
    api("androidx.camera:camera-video:1.4.0")
    api("androidx.camera:camera-view:1.4.0")
    api("androidx.camera:camera-extensions:1.4.0")
    // Accompanist Permission manager Dependency
    implementation("com.google.accompanist:accompanist-permissions:0.33.2-alpha")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.33.2-alpha")
    // JSON De/Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
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
    // Retrofit & OKHTTP3
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup:kotlinpoet:1.12.0")
    dokkaPlugin("org.jetbrains.dokka:android-documentation-plugin:2.0.0")

    // Hilt Test
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
    // Testing Dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-inline:5.0.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    // Android Test Dependencies
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.9")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    // Debug Dependencies
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")
    debugImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    debugImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    debugImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
    debugImplementation("io.mockk:mockk:1.13.9")
    debugImplementation("org.mockito:mockito-core:5.8.0")
    debugImplementation("org.mockito:mockito-inline:5.0.0")
    debugImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    debugImplementation("androidx.test:core:1.5.0")
    debugImplementation("androidx.arch.core:core-testing:2.2.0")
    debugImplementation("app.cash.turbine:turbine:1.0.0")
}
