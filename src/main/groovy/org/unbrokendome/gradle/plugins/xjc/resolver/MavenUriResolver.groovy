package org.unbrokendome.gradle.plugins.xjc.resolver

import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger


class MavenUriResolver implements UriResolver {

    private final Configuration configuration
    private final Logger logger

    private final Map<DependencyResource, URI> resolvedUriCache = [:]


    MavenUriResolver(Configuration configuration, Logger logger) {
        this.configuration = configuration
        this.logger = logger
    }


    @Override
    URI resolve(URI uri) {
        logger.debug 'Resolving URI [{}] as Maven dependency resource.', uri

        def dependency = DependencyResource.parse(uri.schemeSpecificPart)

        def resolvedUri = resolvedUriCache.get(dependency)
        if (resolvedUri == null) {
            resolvedUri = doResolve(dependency)
            if (resolvedUri) {
                resolvedUriCache.put(dependency, resolvedUri)
            }
        }

        return resolvedUri
    }


    private URI doResolve(DependencyResource dependency) {
        def resolvedArtifacts = configuration.resolvedConfiguration.lenientConfiguration.getArtifacts(dependency)
                .findAll { dependency.classifier == null || it.classifier == dependency.classifier }
                .findAll { dependency.ext == null || it.extension == dependency.ext }

        if (dependency.path) {

            def classLoader = new URLClassLoader(resolvedArtifacts*.file*.toURI()*.toURL() as URL[])

            def resourceName = dependency.path.with {
                it.startsWith('/') ? it.substring(1) : it
            }
            def resource = classLoader.getResource(resourceName)
            return resource?.toURI()

        } else {
            return resolvedArtifacts*.file*.toURI().first()
        }
    }
}
