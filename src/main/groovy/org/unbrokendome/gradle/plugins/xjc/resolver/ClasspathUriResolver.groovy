package org.unbrokendome.gradle.plugins.xjc.resolver


class ClasspathUriResolver implements UriResolver {

    @Override
    URI resolve(URI uri) {
        return new URI('maven', "::!${uri.schemeSpecificPart}", null)
    }
}
