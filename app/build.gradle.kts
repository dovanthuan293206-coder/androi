import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val localProperties = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
    .asText.orElse("").map { contents -> Properties().apply { load(contents.reader()) } }
val vilaoKey = localProperties.get().getProperty("vilao.apiKey", "").trim()
require(vilaoKey.isEmpty() || vilaoKey.matches(Regex("[A-Za-z0-9_-]+"))) {
    "Invalid vilao.apiKey in local.properties"
}

android {
    namespace = "com.example.aistudymentor"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.aistudymentor"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "VILAO_API_KEY", "\"$vilaoKey\"")
        }
        release {
            buildConfigField("String", "VILAO_API_KEY", "\"\"")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    // Thư viện Đăng ký / Đăng nhập (Authentication)
    implementation("com.google.firebase:firebase-auth")

// Thư viện Lưu trữ cơ sở dữ liệu (Cloud Firestore)
    implementation("com.google.firebase:firebase-firestore")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

}
