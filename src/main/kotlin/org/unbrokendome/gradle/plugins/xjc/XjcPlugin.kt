package org.unbrokendome.gradle.plugins.xjc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.util.GradleVersion
import org.unbrokendome.gradle.plugins.xjc.internal.GRADLE_VERSION_6_1
import org.unbrokendome.gradle.plugins.xjc.internal.MIN_REQUIRED_GRADLE_VERSION
import java.io.File


class XjcPlugin : Plugin<Project> {

    companion object {

        @JvmStatic
        val XJC_EXTENSION_NAME = "xjc"

        @JvmStatic
        val XJC_TOOL_CONFIGURATION_NAME = "xjcTool"

        @JvmStatic
        val XJC_GLOBAL_CLASSPATH_CONFIGURATION_NAME = "xjcClasspathGlobal"

        @JvmStatic
        val XJC_GLOBAL_CATALOG_RESOLUTION_CONFIGURATION_NAME = "xjcCatalogResolutionGlobal"

        private val DefaultXjcToolDependenciesByVersion = mapOf(
            "2.1" to listOf(        // Supports JAXB -target 2.1  (and 2.0)
                "com.sun.xml.bind:jaxb-xjc:2.1.17",
                "com.sun.xml.bind:jaxb-core:2.1.14",
                "com.sun.xml.bind:jaxb-impl:2.1.17",
                "javax.xml.bind:jaxb-api:2.1"
            ),
            "2.2" to listOf(        // Supports JAXB -target 2.2  (and 2.1 and 2.0)
                "com.sun.xml.bind:jaxb-xjc:2.2.11",
                "com.sun.xml.bind:jaxb-core:2.2.11",
                "com.sun.xml.bind:jaxb-impl:2.2.11",
                "javax.xml.bind:jaxb-api:2.2.12"
            ),
            "2.3" to listOf(        // Supports JAXB -target 2.2  (and 2.1 and 2.0)
                "com.sun.xml.bind:jaxb-xjc:2.3.8",
                "com.sun.xml.bind:jaxb-core:2.3.0.1", // there is no later 2.3 version
                "com.sun.xml.bind:jaxb-impl:2.3.8",  // or 2.3.3
                "javax.xml.bind:jaxb-api:2.3.1"
            ),
            "2.4" to listOf(        // Supports JAXB -target 2.2  (and 2.1 and 2.0)
                "com.sun.xml.bind:jaxb-xjc:2.4.0-b180830.0438",
                "com.sun.xml.bind:jaxb-core:2.3.0.1", // there is no 2.4 version
                "com.sun.xml.bind:jaxb-impl:2.4.0-b180830.0438",
                "javax.xml.bind:jaxb-api:2.4.0-b180830.0359"
            ),
            "3.0" to listOf(        // Supports JAXB -target 3.0  (and 2.3)
                "com.sun.xml.bind:jaxb-xjc:3.0.2",
                "com.sun.xml.bind:jaxb-core:3.0.2",
                "com.sun.xml.bind:jaxb-impl:3.0.2",
                "jakarta.xml.bind:jakarta.xml.bind-api:3.0.1"
            ),
            "4.0" to listOf(        // Supports JAXB -target 3.0  (and 2.3)
                "com.sun.xml.bind:jaxb-xjc:4.0.2",
                "com.sun.xml.bind:jaxb-core:4.0.2",
                "com.sun.xml.bind:jaxb-impl:4.0.2",
                "jakarta.xml.bind:jakarta.xml.bind-api:4.0.0"
            )
        )
    }


    override fun apply(project: Project) {

        check(GradleVersion.current() >= MIN_REQUIRED_GRADLE_VERSION) {
            "The org.unbroken-dome.xjc plugin requires Gradle $MIN_REQUIRED_GRADLE_VERSION or higher."
        }

        val xjcExtension = project.createXjcExtension()
        project.extensions.add(XjcExtension::class.java, XJC_EXTENSION_NAME, xjcExtension)

        val toolClasspathConfiguration = project.createInternalConfiguration(XJC_TOOL_CONFIGURATION_NAME) {
            defaultDependencies { deps ->
                deps.addAll(project.defaultXjcDependencies(xjcExtension.xjcVersion.get()))
            }
        }
        val globalXjcClasspathConfiguration = project.createInternalConfiguration(
            XJC_GLOBAL_CLASSPATH_CONFIGURATION_NAME
        )
        val globalCatalogResolutionConfiguration = project.createInternalConfiguration(
            XJC_GLOBAL_CATALOG_RESOLUTION_CONFIGURATION_NAME
        )

        project.tasks.withType(XjcGenerate::class.java) { task ->
            (task as XjcGeneratorOptions).setFrom(xjcExtension)
            task.toolClasspath.setFrom(toolClasspathConfiguration)
            task.extraArgs.addAll(xjcExtension.extraArgs)
            task.xjcVersionUnsupportedStrategy.set(xjcExtension.xjcVersionUnsupportedStrategy)
        }

        project.plugins.withType(JavaBasePlugin::class.java) {

            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            sourceSets.all { sourceSet ->

                val xjcSourceSetConvention = project.objects.newInstance(
                    XjcSourceSetConvention::class.java, sourceSet, xjcExtension.srcDirName
                )

                (sourceSet as HasConvention).convention.plugins[XJC_EXTENSION_NAME] = xjcSourceSetConvention

                val xjcClasspathConfiguration = project.createInternalConfiguration(
                    xjcSourceSetConvention.xjcClasspathConfigurationName
                ) {
                    extendsFrom(globalXjcClasspathConfiguration)
                }

                val catalogResolutionConfiguration = project.createInternalConfiguration(
                    xjcSourceSetConvention.xjcCatalogResolutionConfigurationName
                ) {
                    extendsFrom(
                        globalCatalogResolutionConfiguration,
                        project.configurations.getByName(sourceSet.compileClasspathConfigurationName)
                    )
                }

                val episodesConfiguration = project.createInternalConfiguration(
                    xjcSourceSetConvention.xjcEpisodesConfigurationName
                )

                val generateTask = project.tasks.register(
                    xjcSourceSetConvention.xjcGenerateTaskName, XjcGenerate::class.java
                ) { task ->
                    task.source.setFrom(xjcSourceSetConvention.xjcSchema)
                    task.bindingFiles.setFrom(xjcSourceSetConvention.xjcBinding)
                    task.urlSources.setFrom(xjcSourceSetConvention.xjcUrl)
                    task.catalogs.setFrom(xjcSourceSetConvention.xjcCatalog)

                    task.pluginClasspath.setFrom(xjcClasspathConfiguration)
                    task.catalogSerializableResolvedArtifact.set(project.provider { XjcGenerate.resolveArtifactsForMavenUri(catalogResolutionConfiguration) })
                    task.episodes.setFrom(episodesConfiguration)

                    task.targetPackage.set(xjcSourceSetConvention.xjcTargetPackage)
                    task.generateEpisode.set(xjcSourceSetConvention.xjcGenerateEpisode)
                    task.extraArgs.addAll(xjcSourceSetConvention.xjcExtraArgs)

                    task.outputDirectory.set(
                        project.layout.buildDirectory.dir("generated/sources/xjc/java/${sourceSet.name}")
                    )
                    task.episodeOutputDirectory.set(
                        project.layout.buildDirectory.dir("generated/resources/xjc/${sourceSet.name}")
                    )
                }

                val xjcOutputDir = generateTask.flatMap { it.outputDirectory }

                // Gradle 6.1 introduced new API and 8.x removed the setOutputDir(File) method, so we can use reflection
                if (GradleVersion.current() >= GRADLE_VERSION_6_1) {
                    val destinationDirectory = reflectMethodAndInvoke(xjcSourceSetConvention.xjcSchema,
                        "getDestinationDirectory")
                    reflectMethodAndInvoke(destinationDirectory!!,
                        "set", arrayOf(Provider::class.java), arrayOf(xjcOutputDir))
                    // The intention is to maintain compatibility with Gradle 5.6+ and 8.x
                    //xjcSourceSetConvention.xjcSchema.destinationDirectory.set(xjcOutputDir)
                    sourceSet.java.srcDir(xjcOutputDir)
                } else {
                    reflectMethodAndInvoke(xjcSourceSetConvention.xjcSchema,
                        "setOutputDir", arrayOf(File::class.java), arrayOf(project.file(generateTask.flatMap { it.outputDirectory })))
                    // The intention is to maintain compatibility with Gradle 5.6+ and 8.x
                    //xjcSourceSetConvention.xjcSchema.outputDir =
                    //    project.file(generateTask.flatMap { it.outputDirectory })
                    sourceSet.java.srcDir(xjcOutputDir)
                }

                sourceSet.resources.srcDir(
                    generateTask.flatMap { it.episodeOutputDirectory }
                )
            }
        }
    }


    // Perform method invocation via reflection API (to maintain compile time compatability with older Gradle versions)
    private fun reflectMethodAndInvoke(target: Any, methodName: String, methodParameters: Array<Class<*>> = emptyArray(), values: Array<Any?> = emptyArray()): Any? {
        val method = if(methodParameters.isNotEmpty()) target.javaClass.getMethod(methodName, *methodParameters) else target.javaClass.getMethod(methodName)
        return if(values.isNotEmpty()) method.invoke(target, *values) else method.invoke(target)
    }


    private fun Project.defaultXjcDependencies(xjcVersion: String): List<Dependency> =
        checkNotNull(DefaultXjcToolDependenciesByVersion[xjcVersion]) {
            "Invalid XJC version: \"$version\". Valid values are: ${DefaultXjcToolDependenciesByVersion.keys}"
        }.map { notation ->
            project.dependencies.create(notation)
        }


    private fun Project.createInternalConfiguration(
        name: String, configureAction: Configuration.() -> Unit = { }
    ) =
        configurations.create(name) { configuration ->
            configuration.isVisible = false
            configuration.configureAction()
        }
}
