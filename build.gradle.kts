plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint") version "12.0.3"
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    repositories {
        // Required to download KtLint
        mavenCentral()
    }

    tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask> {
        // Configure KtLint to automatically format Kotlin files on build
        enabled = true
    }
}

android {
    namespace = "co.verifik.wallet"
    compileSdk = 34

    defaultConfig {
        applicationId = "co.verifik.wallet"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.2"

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
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.7"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        viewBinding = true
    }
    configurations {
        all {
            exclude(module = "bcprov-jdk15on")
        }
    }
}

dependencies {
    // A good library for camera
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    // A good library for QR code scanning
    implementation("io.github.zxing-cpp:android:2.2.0")
    implementation("com.google.zxing:core:3.5.1")
    // Included in the SDK
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    // A good library to handle permissions
    implementation("com.karumi:dexter:6.2.3")
    // Animated progress bars
    implementation("com.github.ybq:Android-SpinKit:1.4.0")
    // Show an image in a circle
    implementation("de.hdodenhof:circleimageview:3.1.0")
    // Lottie animations
    implementation("com.airbnb.android:lottie:6.3.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    //Viewpager2
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    //QR code scanning -directly from google mlkit-
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.mlkit:vision-common:17.3.0")
    //Flexbox for dynamic cells sizes on recyclerview
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    //Web3 implementation for wallet
    implementation("org.web3j:core:5.0.0")
    implementation("org.sol4k:sol4k:0.4.2")
    implementation("com.paymennt:solanaj:0.0.7")

    //Retrofit to get endpoints
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    //Moshi to convert json to kotlin objects
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    //Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.activity:activity:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.fragment:fragment-ktx:1.5.6")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
