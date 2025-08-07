plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp) // Add KSP plugin
    alias(libs.plugins.hilt) // Add Hilt plugin
}

android {
    namespace = "com.ahm.mytime"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ahm.mytime"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
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
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3.android)

    // For background data fetching
    implementation(libs.androidx.work.runtime.ktx)

    // For parsing HTML to get prayer times
    implementation("org.jsoup:jsoup:1.17.2")

    // For Islamic Calendar conversion
    implementation("com.github.msarhan:ummalqura-calendar:1.1.1")

    // Retrofit for type-safe HTTP calls
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Room dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx) // Kotlin Extensions and Coroutines support for Room
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.hilt.common) // Add Room Paging integration
    ksp(libs.androidx.room.compiler) // Room compiler

    // Hilt dependencies
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    // Hilt-WorkManager Integration
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
}