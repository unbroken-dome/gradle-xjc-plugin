package org.unbrokendome.gradle.plugins.xjc.resolver

import java.net.URI


interface UriResolver {

    fun resolve(uri: URI): URI
}
