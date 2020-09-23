pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}

rootProject.name = "episodes"

include(
    "episode-consumer",
    "episode-producer"
)
