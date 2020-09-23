package org.unbrokendome.gradle.plugins.xjc.resolver

import java.net.URI


object ClasspathUriResolver : UriResolver {

    override fun resolve(uri: URI): URI =
        URI("maven", "::!${uri.schemeSpecificPart}", null)
}
