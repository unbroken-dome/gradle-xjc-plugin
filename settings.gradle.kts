pluginManagement {

    val kotlinVersion: String by settings

    resolutionStrategy.eachPlugin {
        if (requested.id.namespace == "org.jetbrains.kotlin") {
            useVersion(kotlinVersion)
        }
    }
}

rootProject.name = "gradle-xjc-plugin"
