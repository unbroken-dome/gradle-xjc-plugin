pluginManagement {

    val kotlinVersion: String by settings
    val testSetsVersion: String by settings

    // Still needed while we support building this project before Gradle 7.6
    resolutionStrategy.eachPlugin {
        if (requested.id.namespace == "org.jetbrains.kotlin") {
            useVersion(kotlinVersion)
        }
        if (requested.id.id == "org.unbroken-dome.test-sets") {
            useVersion(testSetsVersion)
        }
    }
}

rootProject.name = "gradle-xjc-plugin"

if(System.getProperty("excludeDocsTasks") == null) {
    include(":docs")
} else {
    logger.warn("systemProperty \"excludeDocsTasks\" is present, causing project :docs exclusion")
}
