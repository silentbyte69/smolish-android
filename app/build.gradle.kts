import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val releaseKeystore: String? = localProps.getProperty("smolish.keystore")
val releaseStorePassword: String? = localProps.getProperty("smolish.keystorePassword")
val releaseKeyAlias: String? = localProps.getProperty("smolish.keyAlias")
val releaseKeyPassword: String? = localProps.getProperty("smolish.keyPassword")

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

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            if (releaseKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
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
