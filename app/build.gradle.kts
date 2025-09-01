plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
<<<<<<< HEAD
//    alias(libs.plugins.secrets.gradle.plugin)
=======
    alias(libs.plugins.secrets.gradle.plugin)
>>>>>>> d58b837438c1a118847fd3a3cce42afcf7f2b944
}

android {
    namespace = "com.example.photomapapp"
    compileSdk = 36 // Changed to match your targetSdk for consistency and stability.

    buildFeatures {
        buildConfig = true
    }


    defaultConfig {
        applicationId = "com.example.photomapapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MAPS_API_KEY", "\"AIzaSyCa6uipgjmFeucnr_pPiZATI_r_8opSTEw\"")

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}
dependencies {
    // AndroidX libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
<<<<<<< HEAD

    // Google Maps SDK
    implementation(libs.google.play.services.maps)
    implementation(libs.google.play.services.location)

    // Image loading library
    implementation(libs.coil.kt)

    // Photo EXIF data library
    implementation(libs.androidx.exifinterface)

    // PhotoView for zoomable image in dialog
    implementation(libs.photoview)

    // Test dependencies
=======
    implementation(libs.androidx.exifinterface)

    // Google Services
    implementation(libs.google.play.services.maps)
    implementation(libs.google.services.location)

    // Gson library for JSON parsing
    implementation(libs.gson)

    // Testing libraries
>>>>>>> d58b837438c1a118847fd3a3cce42afcf7f2b944
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}