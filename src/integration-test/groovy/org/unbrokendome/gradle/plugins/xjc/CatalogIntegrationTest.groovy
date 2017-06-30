package org.unbrokendome.gradle.plugins.xjc

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class CatalogIntegrationTest extends Specification {

    @Rule TemporaryFolder testProjectDir

    def setup() {
        new FileTreeBuilder(testProjectDir.root).with {

            file('build.gradle', '''
                plugins { id 'org.unbroken-dome.xjc' }
                
                repositories { jcenter() }
                
                dependencies {
                    compileOnly 'javax.servlet:javax.servlet-api:3.1.0'
                }

                xjcGenerate {
                    source = fileTree('src/main/schema') { include '*.xsd' }
                    catalogs = fileTree('src/main/catalog') { include '*.cat' }
                }
                ''')

            dir('src') {
                dir('main') {
                    dir('schema') {
                        file('example.xsd', '''
                            <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                                    xmlns:addr="http://www.example.com/address"
                                    targetNamespace="http://www.example.com/person"
                                    elementFormDefault="qualified">

                                <xsd:import namespace="http://java.sun.com/xml/ns/javaee"
                                            schemaLocation="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" />

                            </xsd:schema>
                            ''')
                    }
                }
            }
        }
    }


    def "Resolve Maven URI in catalog"() {
        given:
            new FileTreeBuilder(testProjectDir.root).with {
                dir('src/main/catalog') {
                    file('catalog.cat', '''
                            REWRITE_SYSTEM "http://java.sun.com/xml/ns/javaee" "maven:javax.servlet:javax.servlet-api:jar::3.1.0!/javax/servlet/resources"
                            '''.stripIndent())
                }
            }

        when:
            def result = GradleRunner.create()
                    .withProjectDir(testProjectDir.root)
                    .withPluginClasspath()
                    .withArguments('build', '--info', '--stacktrace')
                    .withDebug(true)
                    .build()

        then:
            result.task(':xjcGenerate').outcome == TaskOutcome.SUCCESS
    }


    def "Resolve classpath URI in catalog"() {
        given:
            new FileTreeBuilder(testProjectDir.root).with {
                dir('src/main/catalog') {
                    file('catalog.cat', '''
                            REWRITE_SYSTEM "http://java.sun.com/xml/ns/javaee" "classpath:/javax/servlet/resources"
                            '''.stripIndent())
                }
            }

        when:
            def result = GradleRunner.create()
                    .withProjectDir(testProjectDir.root)
                    .withPluginClasspath()
                    .withArguments('build', '--debug', '--stacktrace')
                    .withDebug(true)
                    .forwardOutput()
                    .build()

        then:
            result.task(':xjcGenerate').outcome == TaskOutcome.SUCCESS
    }
}
