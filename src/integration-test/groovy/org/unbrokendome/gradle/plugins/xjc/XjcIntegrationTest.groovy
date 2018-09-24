package org.unbrokendome.gradle.plugins.xjc

import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.gradle.testkit.runner.UnexpectedBuildResultException
import org.gradle.testkit.runner.UnexpectedBuildSuccess
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
                    .withDebug(true)
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
    
    
    def "build remote wsdl"() {
        given:
            def schemaFolder = testProjectDir.newFolder('src', 'main', 'schema')
           
            buildFile << """
                plugins {
                    id 'org.unbroken-dome.xjc'
                }

                xjcGenerate {
                    source = fileTree('src/main/schema') { include '*.url' }
                }
            """
        when:
            File urlFile = testProjectDir.newFile('src/main/schema/vat.url')
            urlFile << "http://ec.europa.eu/taxation_customs/vies/checkVatService.wsdl"
            def result = GradleRunner.create()
                    .withProjectDir(testProjectDir.root)
                    .withPluginClasspath()
                    .withArguments('xjcGenerate', '--stacktrace')
                    .withDebug(true)
                    .build()
            def generatedFiles = new File(testProjectDir.root, 'build/xjc/generated-sources').listFiles()*.name
            println generatedFiles
        then:
            result.task(':xjcGenerate').outcome == TaskOutcome.SUCCESS
        and:
            expect generatedFiles, contains('eu')
    }
    
    def "broken url"() {
        given:
            def schemaFolder = testProjectDir.newFolder('src', 'main', 'schema')
            File urlFile = testProjectDir.newFile('src/main/schema/vat.url')
            urlFile << "\\ec.europa.eu/taxation_customs/vies/checkVatService.wsdl"
            
            buildFile << """
                plugins {
                    id 'org.unbroken-dome.xjc'
                }

                xjcGenerate {
                    source = fileTree('src/main/schema') { include '*.url' }
                }
            """
        when:
            def result = GradleRunner.create()
                    .withProjectDir(testProjectDir.root)
                    .withPluginClasspath()
                    .withArguments('xjcGenerate', '--stacktrace')
                    .withDebug(true)
                    .buildAndFail()
        then:
            notThrown UnexpectedBuildSuccess
            result.task(':xjcGenerate').outcome == TaskOutcome.FAILED
            
    }
}
