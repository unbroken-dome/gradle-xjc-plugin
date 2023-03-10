package org.unbrokendome.gradle.plugins.xjc.resolver

import org.gradle.api.logging.Logging
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.xjc.internal.SerializableResolvedArtifact
import java.net.URI
import java.net.URLClassLoader


class MavenUriResolver(
    private val artifacts: List<SerializableResolvedArtifact>
) : UriResolver {

    companion object {
        private val logger = Logging.getLogger(MavenUriResolver::class.java)
    }

    private val resolvedUriCache: MutableMap<DependencyResource, URI> = HashMap()


    override fun resolve(uri: URI): URI {
        logger.debug("Resolving URI [{}] as Maven dependency resource.", uri)

        val dependency = DependencyResource.parse(uri.schemeSpecificPart)
        logger.debug("Parsed dependency for URI resolution: {}", dependency)

        return resolvedUriCache.computeIfAbsent(dependency, this::doResolve)
    }


    private fun doResolve(dependency: DependencyResource): URI {

        val matchingArtifacts = artifacts.asSequence()
            .filter {
                (dependency.group == null || it.group == dependency.group) &&
                        (dependency.name == null || it.name == dependency.name) &&
                        (dependency.ext == null || it.extension == dependency.ext) &&
                        (dependency.version == null || it.version == dependency.version) &&
                        (dependency.classifier == null || it.classifier == dependency.classifier)
            }
            .ifEmpty {
                throw IllegalArgumentException("Could not resolve dependency: $dependency")
            }

        val path = dependency.path

        return if (path != null) {
            if(logger.isDebugEnabled) {     // This is useful visibility for users
                matchingArtifacts.forEach {
                    logger.debug("MavenUriResolver matchingArtifacts={} for {}", it, dependency)
                }
            }
            val classLoader = URLClassLoader(
                matchingArtifacts.map { it.file.toURI().toURL() }.toList().toTypedArray()
            )
            val resourceName = path.removePrefix("/")
            classLoader.use {
                classLoader.getResource(resourceName)?.toURI()
                    ?: throw IllegalArgumentException(
                        "Could not resolve resource \"$resourceName\" from classpath: ${classLoader.urLs.toList()}"
                    )
            }

        } else {
            val artifacts = matchingArtifacts.toList()
            if(artifacts.size > 1) {
                for((index,it) in artifacts.withIndex()) {
                    logger.warn("MavenUriResolver multiple matching artifacts found, only index 0 is selected[{}]: {}", index, it)
                }
            }
            artifacts[0].file.toURI()
        }
    }
}
