package org.unbrokendome.gradle.plugins.xjc

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification


class XjcPluginTest extends Specification {

    def project = ProjectBuilder.builder().build()


    def "Should create xjc extension"() {
        when:
            project.apply plugin: XjcPlugin

        then:
            project.extensions.findByName(XjcPlugin.XJC_EXTENSION_NAME) instanceof XjcExtension
    }


    def "Should create XjcGenerate task"() {
        when:
            project.apply plugin: XjcPlugin

        then:
            project.tasks.findByName(XjcPlugin.XJC_GENERATE_TASK_NAME) instanceof XjcGenerate
    }


    def "Output directory should be set by default"() {
        when:
            project.apply plugin: XjcPlugin
        and:
            project.evaluate()

        then:
            project.tasks[XjcPlugin.XJC_GENERATE_TASK_NAME].outputDirectory != null
    }
}
