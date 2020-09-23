package org.unbrokendome.gradle.plugins.xjc.resolver

import org.apache.xml.resolver.CatalogManager
import org.apache.xml.resolver.tools.CatalogResolver
import org.slf4j.LoggerFactory
import java.net.URI


class ExtensibleCatalogResolver(
    catalogManager: CatalogManager,
    private val uriResolvers: Map<String, UriResolver>
) : CatalogResolver(catalogManager) {

    companion object {
        private val logger = LoggerFactory.getLogger(ExtensibleCatalogResolver::class.java)
    }


    override fun getResolvedEntity(publicId: String?, systemId: String?): String? {

        logger.debug("Resolving publicId [{}], systemId [{}]", publicId, systemId)

        val superResolvedEntity = super.getResolvedEntity(publicId, systemId)
        logger.debug(
            "Parent resolver has resolved publicId [{}], systemId [{}] to [{}]",
            publicId, systemId, superResolvedEntity
        )

        return generateSequence(
            seedFunction = { (superResolvedEntity ?: systemId)?.let(::URI) },
            nextFunction = { uri ->
                uriResolvers[uri.scheme]?.let { resolver ->
                    try {
                        resolver.resolve(uri)
                    } catch (ex: Exception) {
                        logger.warn(
                            "Resolver for the \"{}\" scheme could not resolve system ID: [{}]",
                            uri.scheme, systemId, ex
                        )
                        null
                    }
                }
            }
        ).lastOrNull()
            ?.toString()
            ?: superResolvedEntity
    }
}
