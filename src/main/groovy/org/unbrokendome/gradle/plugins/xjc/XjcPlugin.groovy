package org.unbrokendome.gradle.plugins.xjc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar


@SuppressWarnings("GrMethodMayBeStatic")
class XjcPlugin implements Plugin<Project> {

    static final String XJC_EXTENSION_NAME = 'xjc'
    static final String XJC_EPISODE_CONFIGURATION_NAME = 'xjcEpisode'
    static final String XJC_CLASSPATH_CONFIGURATION_NAME = 'xjcClasspath'
    static final String XJC_CATALOG_RESOLUTION_CONFIGURATION_NAME = 'xjcCatalogResolution'
    static final String XJC_GENERATE_TASK_NAME = 'xjcGenerate'


    @Override
    void apply(Project project) {

        project.plugins.apply JavaPlugin

        project.extensions.create XJC_EXTENSION_NAME, XjcExtension

        def episodesConfiguration = createInternalConfiguration(project, XJC_EPISODE_CONFIGURATION_NAME)
        def pluginClasspathConfiguration = createInternalConfiguration(project, XJC_CLASSPATH_CONFIGURATION_NAME)
        def catalogResolutionClasspathConfiguration = createInternalConfiguration(project, XJC_CATALOG_RESOLUTION_CONFIGURATION_NAME)
        project.tasks.withType(XjcGenerate) { XjcGenerate task ->
            task.episodes = episodesConfiguration
            task.pluginClasspath = pluginClasspathConfiguration
            task.catalogResolutionClasspath = catalogResolutionClasspathConfiguration
        }

        def xjcTask = createXjcGenerateTask(project)

        handleIncludeInMainCompilation(project, xjcTask)
        handleIncludeEpisodeFileInJar(project, xjcTask)
    }


    private XjcGenerate createXjcGenerateTask(Project project) {
        def xjcTask = project.tasks.create XJC_GENERATE_TASK_NAME, XjcGenerate

        xjcTask.source = project.fileTree('src/main/schema') { include '*.xsd' }
        xjcTask.bindingFiles = project.fileTree('src/main/schema') { include '*.xjb' }
        xjcTask.urlSources  = project.fileTree('src/main/schema') { include '*.url' }
        xjcTask.conventionMapping.with {
            map('outputDirectory') { project.file("${project.buildDir}/xjc/generated-sources") }
            map('episodeTargetFile') { project.file("${project.buildDir}/xjc/sun-jaxb.episode") }
        }
        xjcTask
    }


    private Configuration createInternalConfiguration(Project project, String name) {
        project.configurations.create(name) { Configuration c ->
            c.visible = false
        }
    }


    private void handleIncludeInMainCompilation(Project project, XjcGenerate xjcTask) {
        project.afterEvaluate {
            def xjcExtension = project.extensions.getByType XjcExtension
            if (xjcExtension.includeInMainCompilation) {
                def sourceSets = project.sourceSets as SourceSetContainer
                sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).java {
                    srcDir xjcTask.outputDirectory
                }

                def compileJavaTask = project.tasks.getByName JavaPlugin.COMPILE_JAVA_TASK_NAME
                compileJavaTask.dependsOn xjcTask

                def catalogResolutionConfiguration = project.configurations.getByName XJC_CATALOG_RESOLUTION_CONFIGURATION_NAME

                // If the catalog resolution classpath hasn't been modified, make it extend from the compileClasspath
                // (note that we can only do this if includeInMainCompilation is true, otherwise we would get a
                // circular dependency - see issue #11)
                if (catalogResolutionConfiguration.extendsFrom.empty && catalogResolutionConfiguration.dependencies.empty) {
                    //noinspection GrDeprecatedAPIUsage
                    catalogResolutionConfiguration.extendsFrom(
                            project.configurations.findByName('compileClasspath')
                                    ?: project.configurations.findByName(JavaPlugin.COMPILE_CONFIGURATION_NAME))
                }
            }
        }
    }


    private void handleIncludeEpisodeFileInJar(Project project, XjcGenerate xjcTask) {
        project.afterEvaluate {
            def xjcExtension = project.extensions.getByType XjcExtension
            if (xjcExtension.includeEpisodeFileInJar) {
                def jarTask = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar
                jarTask.into('META-INF') {
                    from xjcTask.episodeTargetFile
                }
                jarTask.dependsOn xjcTask
            }
        }
    }
}
