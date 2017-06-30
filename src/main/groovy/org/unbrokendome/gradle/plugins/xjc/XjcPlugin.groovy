package org.unbrokendome.gradle.plugins.xjc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar


class XjcPlugin implements Plugin<Project> {

    public static final String XJC_EXTENSION_NAME = 'xjc'
    public static final String XJC_EPISODE_CONFIGURATION_NAME = 'xjcEpisode'
    public static final String XJC_CLASSPATH_CONFIGURATION_NAME = 'xjcClasspath'
    public static final String XJC_GENERATE_TASK_NAME = 'xjcGenerate'


    @Override
    void apply(Project project) {

        project.plugins.apply JavaPlugin

        project.extensions.create XJC_EXTENSION_NAME, XjcExtension

        def xjcTask = createXjcGenerateTask(project)

        handleIncludeInMainCompilation(project, xjcTask)
        handleIncludeEpisodeFileInJar(project, xjcTask)
    }


    private XjcGenerate createXjcGenerateTask(Project project) {
        def xjcTask = project.tasks.create XJC_GENERATE_TASK_NAME, XjcGenerate
        xjcTask.group = 'code generation'
        xjcTask.description = 'Generates Java classes from XML schema using the XJC binding compiler'
        xjcTask.source = project.fileTree('src/main/schema') { include '*.xsd' }
        xjcTask.bindingFiles = project.fileTree('src/main/schema') { include '*.xjb' }
        xjcTask.episodes = project.configurations.create XJC_EPISODE_CONFIGURATION_NAME
        xjcTask.pluginClasspath = project.configurations.create XJC_CLASSPATH_CONFIGURATION_NAME
        xjcTask.conventionMapping.with {
            map('outputDirectory') { project.file("${project.buildDir}/xjc/generated-sources") }
            map('episodeTargetFile') { project.file("${project.buildDir}/xjc/sun-jaxb.episode") }
            map('catalogResolutionClasspath') {
                project.configurations.findByName('compileClasspath') ?: project.configurations.findByName('compile')
            }
        }
        xjcTask
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
