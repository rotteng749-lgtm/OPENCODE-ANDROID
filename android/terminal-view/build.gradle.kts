plugins {
    id("com.android.library")
}

/**
 * TerminalView, vendored from termux-app (Apache-2.0).
 *
 * This is the Android View that renders a TerminalSession: glyph grid, cursor,
 * text selection, IME handling and touch gestures. It is hosted from Compose with
 * AndroidView rather than rewritten, because getting the renderer right is exactly
 * the hard part and Termux already did it.
 */
android {
    namespace = "com.termux.view"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
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
    api(project(":terminal-emulator"))
    implementation("androidx.annotation:annotation:1.9.0")
}
