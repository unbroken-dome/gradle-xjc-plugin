package org.unbrokendome.gradle.plugins.xjc.resolver

import spock.lang.Specification


class DependencyResourceTest extends Specification {

    def "Group ID and artifact ID only"() {
        given:
            def notation = 'com.example.group:example-artifact'
        when:
            def dep = DependencyResource.parse(notation)
        then:
            dep.group == 'com.example.group'
            dep.name == 'example-artifact'
    }


    def "Group ID and artifact ID with path"() {
        given:
            def notation = 'com.example.group:example-artifact!/path/to/resource'
        when:
            def dep = DependencyResource.parse(notation)
        then:
            dep.group == 'com.example.group'
            dep.name == 'example-artifact'
            dep.path == '/path/to/resource'
    }


    def "Group ID, artifact ID and type"() {
        given:
            def notation = 'com.example.group:example-artifact:jar'
        when:
            def dep = DependencyResource.parse(notation)
        then:
            dep.group == 'com.example.group'
            dep.name == 'example-artifact'
            dep.ext == 'jar'
    }


    def "Group ID, artifact ID and version"() {
        given:
            def notation = 'com.example.group:example-artifact:::1.2.3'
        when:
            def dep = DependencyResource.parse(notation)
        then:
            dep.group == 'com.example.group'
            dep.name == 'example-artifact'
            dep.version == '1.2.3'
    }
}
