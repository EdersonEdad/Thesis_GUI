pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io") // ✅ Correct syntax for Kotlin DSL
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // ✅ Correct syntax for Kotlin DSL

    }
}

rootProject.name = "Tomato Fruit Diseases Scanner"
include(":app")
