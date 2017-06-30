package org.unbrokendome.gradle.plugins.xjc.resolver

import groovy.transform.Immutable
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.specs.Spec

@Immutable
class DependencyResource implements ModuleVersionIdentifier, ModuleIdentifier, Spec<Dependency> {

    private static final PATTERN = ~/^(?x)
        (?<group>[^:]*?):(?<name>[^:]*?)
        (?::(?<ext>[^:]*?)
            (?::(?<classifier>[^:]*?)
                (?::(?<version>[^:]*?))?
            )?
        )?
        (?:!(?<path>.*))?
        $/


    String group
    String name
    String version
    String classifier
    String ext
    String path


    static DependencyResource parse(String input) {
        def matcher = PATTERN.matcher(input)
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Error parsing dependency notation [$input]: does not match expected format")
        }

        def params = ['group', 'name', 'ext', 'classifier', 'version', 'path']
                .collectEntries { [it, matcher.group(it)] }
                .findAll { k, v -> v } as HashMap

        return new DependencyResource(params)
    }


    @Override
    boolean isSatisfiedBy(Dependency dependency) {

        if (this.group != null && this.group != dependency.group) {
            return false
        }

        if (this.name != null && this.name != dependency.name) {
            return false
        }

        if (this.version != null) {
            if (dependency instanceof ModuleVersionSelector) {
                if (!dependency.matchesStrictly(this)) {
                    return false
                }
            } else {
                if (this.version != dependency.version) {
                    return false
                }
            }
        }

        return true
    }


    @Override
    ModuleIdentifier getModule() {
        this
    }
}
