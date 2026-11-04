plugins {
    id("com.android.library")
}

/**
 * The terminal emulator, vendored from termux-app (Apache-2.0).
 *
 * Only the emulator core and its PTY bridge are kept. Everything Termux-specific
 * (services, preferences, bootstrap) lives in termux-shared, which is GPLv3 and is
 * deliberately not copied. The JNI surface keeps the original package name so the
 * native symbol names in `termux.c` stay untouched.
 */
android {
    namespace = "com.termux.emulator"
    compileSdk = 35
    ndkVersion = "29.0.14206865"

    defaultConfig {
        minSdk = 28
        ndk {
            // The app only ships these two ABIs; building the other two would just
            // add minutes to every clean build.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            ndkBuild {
                cFlags += listOf("-std=c11", "-Wall", "-Wextra", "-Os", "-fno-stack-protector")
            }
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.0")
}
