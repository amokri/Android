plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // For parsing HTML to get prayer times
    implementation("org.jsoup:jsoup:1.17.2")

    // For Islamic Calendar conversion
    implementation("com.github.msarhan:ummalqura-calendar:1.1.1")

    // Retrofit for type-safe HTTP calls
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
}