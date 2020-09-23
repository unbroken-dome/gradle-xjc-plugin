pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}

rootProject.name = "complete"

include(
    "consumer",
    "library"
)
