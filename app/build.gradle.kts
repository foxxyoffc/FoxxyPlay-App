import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use {
        load(it)
    }
}

val musicApiClientId = localProperties
    .getProperty("musicapi.clientId", "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

tasks.configureEach {
    if (name.startsWith("processDeveloper") && name.endsWith("GoogleServices")) {
        enabled = false
    }

    if (
        name.startsWith("processPublic") &&
        name.endsWith("GoogleServices") &&
        !file("src/public/google-services.json").isFile
    ) {
        enabled = false
    }
}

android {
    namespace = "com.foxxy.play"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.foxxy.play"
        minSdk = 26
        targetSdk = 35
        versionCode = 41
        versionName = "1.0.40"
        buildConfigField(
            "String",
            "MUSICAPI_CLIENT_ID",
            "\"$musicApiClientId\""
        )

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "edition"

    productFlavors {
        create("public") {
            dimension = "edition"
            applicationId = "com.foxxy.play"
            resValue("string", "app_name", "FoxxyPlay")
        }

        create("developer") {
            dimension = "edition"
            applicationId = "com.foxxy.play.developer"
            resValue("string", "app_name", "FoxxyPlay Developer")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    add("publicImplementation", platform("com.google.firebase:firebase-bom:34.9.0"))
    add("publicImplementation", "com.google.firebase:firebase-analytics")
    add("publicImplementation", "com.google.firebase:firebase-messaging")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-common:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")
    kapt("androidx.room:room-compiler:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    implementation("androidx.room:room-runtime:2.8.3")

    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
