plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.example.soukify"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.soukify"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Configure ABI filtering for universal APK compatibility
        ndk {
            abiFilters.addAll(listOf(
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64"
            ))
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }

    // Disable APK splits to ensure compatibility with all devices
    splits {
        abi {
            isEnable = false
        }
    }
}

dependencies {
    // AndroidX Core Libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // UI Components
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Firebase (using BOM for version management)
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.firebase.crashlytics)

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Room Database (for offline caching)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation(libs.room.runtime)
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    annotationProcessor(libs.room.compiler)

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Cloudinary for media storage
    implementation("com.cloudinary:cloudinary-android:3.0.2")

    // Security
    implementation("at.favre.lib:bcrypt:0.10.2")

    // OpenStreetMap (osmdroid)
    implementation("org.osmdroid:osmdroid-android:6.1.14")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}