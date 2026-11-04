pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OpenCodeAndroid"

include(":app")
include(":core:common")
include(":core:design")
include(":core:engine")
include(":core:api")

// Vendored from termux-app (Apache-2.0). The emulator owns the PTY bridge; the
// view renders the glyph grid. Kept as separate modules so the licences and the
// upstream boundary stay obvious.
include(":terminal-emulator")
include(":terminal-view")
