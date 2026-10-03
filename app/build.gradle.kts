plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "wtf.cuteslavicboy.smolishapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "wtf.cuteslavicboy.smolishapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "1.2"
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
    implementation(libs.androidx.core.ktx)
    implementation("androidx.activity:activity-ktx:1.9.3")
}
