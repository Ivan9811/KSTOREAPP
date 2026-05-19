plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ucompensar.kstoreapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ucompensar.kstoreapp"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ Lee local.properties con Kotlin puro, sin java.util.Properties
        val googleClientId = rootProject.file("local.properties")
            .readLines()
            .find { it.startsWith("GOOGLE_WEB_CLIENT_ID=") }
            ?.substringAfter("=")
            ?: error("GOOGLE_WEB_CLIENT_ID no encontrado en local.properties")

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
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
        buildConfig  = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)

    // Ktor
    implementation(libs.ktor.client.android)

    // Serialización
    implementation(libs.kotlinx.serialization.json)

    // Google Sign-In
    implementation(libs.google.id)
    implementation(libs.credential.manager)
    implementation(libs.credential.manager.play)

    // Biometría
    implementation(libs.biometric)

    // Imágenes
    implementation(libs.coil)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Maps
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
}