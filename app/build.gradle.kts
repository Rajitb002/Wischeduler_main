plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.wischeduler"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.wischeduler"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {
    implementation(libs.google.oauth.client.jetty)
    implementation(libs.gms.google.services)
    implementation(libs.google.services)
    implementation (libs.retrofit2.retrofit)
    implementation (libs.converter.gson)

    implementation (libs.play.services.auth)
    implementation (libs.google.api.client.android) // Required for Google API client
    implementation (libs.google.api.client.gson) // JSON parsing for Google API client
    implementation (libs.google.api.services.calendar)
    implementation(libs.google.http.client.gson) // Add this line

    // To avoid conflicts in libraries
    implementation(libs.listenablefuture)

    implementation("com.google.api-client:google-api-client-android:1.23.0") {
        exclude(group = "org.apache.httpcomponents")
    }

    implementation (libs.com.google.firebase.firebase.auth)  // Example version, check for the latest version
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.google.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
