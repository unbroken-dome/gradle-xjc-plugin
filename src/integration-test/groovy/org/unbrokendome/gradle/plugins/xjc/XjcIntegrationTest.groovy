package org.unbrokendome.gradle.plugins.xjc

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Files

import static spock.util.matcher.HamcrestSupport.*
import static org.hamcrest.Matchers.*


class XjcIntegrationTest extends Specification {

    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }

    def "run build"() {
        given:
            buildFile << """
                plugins {
                    id 'org.unbroken-dome.xjc'
                }
                xjc {
                }
            """

            def schemaFolder = testProjectDir.newFolder('src', 'main', 'schema')

            getClass().getResourceAsStream('/test-project/jaxb-example.xsd').withCloseable { input ->
                Files.copy(input, new File(schemaFolder, 'jaxb-example.xsd').toPath())
            }

        when:
            def result = GradleRunner.create()
                    .withProjectDir(testProjectDir.root)
                    .withPluginClasspath()
                    .withArguments('xjc', '--stacktrace')
                    .build()
            def generatedFiles = new File(testProjectDir.root, 'build/xjc/generated-sources/generated').listFiles()*.name

        then:
            result.task(':xjcGenerate').outcome == TaskOutcome.SUCCESS
        and:
            expect generatedFiles, containsInAnyOrder(
                    'PurchaseOrderType.java',
                    'USAddress.java',
                    'Items.java',
                    'ObjectFactory.java')
    }
}
