plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.beatify"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.beatify"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // REMOVED: ndk block causing
        // conflict with splits
        // ndk {
        //     abiFilters.add("armeabi-v7a")
        //     abiFilters.add("arm64-v8a")
        //     abiFilters.add("x86")
        //     abiFilters.add("x86_64")
        // }
    }

    // Splits configuration to reduce APK size
    splits {

        abi {
            isEnable = true
            reset()
            // Include the architectures you want to support
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            // If true, also generates a "universal" APK that includes everything
            isUniversalApk = false

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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation("androidx.fragment:fragment-ktx:1.6.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // ADDED: Media dependency for MediaSession and Notification style
    implementation("androidx.media:media:1.6.0")

    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("io.github.junkfood02.youtubedl-android:library:0.18.0")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.0")

    testImplementation(libs.junit)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}