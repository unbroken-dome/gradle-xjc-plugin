pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}

rootProject.name = "catalogs"

include(
    "consumer",
    "schema-library"
)
