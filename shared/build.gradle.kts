plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        namespace = "com.impactomsn.shared"
        // Keep the shared library usable with a stock Android SDK. The app
        // itself can continue targeting its newer compile SDK independently.
        compileSdk = 34
        minSdk = 24
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosX64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.sqldelight.runtime)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android.driver)
        }
        nativeMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

sqldelight {
    databases {
        create("ImpactoMsnDatabase") {
            packageName.set("com.impactomsn.shared.db")
        }
    }
}
