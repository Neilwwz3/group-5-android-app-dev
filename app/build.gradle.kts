import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "hk.hkucs.group_6_android_app_dev"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "hk.hkucs.group_6_android_app_dev"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // G7: TMDB key lives only in the gitignored local.properties. Builds without it
        // (including CI) get an empty key and hide the online search entry.
        val localProps = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }
        val tmdbKey = localProps.getProperty("tmdbApiKey") ?: ""
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.fragment)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}