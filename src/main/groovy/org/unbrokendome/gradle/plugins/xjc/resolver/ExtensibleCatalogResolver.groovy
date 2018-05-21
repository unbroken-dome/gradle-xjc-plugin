package org.unbrokendome.gradle.plugins.xjc.resolver

import org.apache.xml.resolver.CatalogManager
import org.apache.xml.resolver.tools.CatalogResolver
import org.gradle.api.logging.Logger


class ExtensibleCatalogResolver extends CatalogResolver {

    private final Logger logger
    private final Map<String, ? extends UriResolver> uriResolvers


    ExtensibleCatalogResolver(CatalogManager catalogManager, Logger logger,
                              Map<String, ? extends UriResolver> uriResolvers) {
        super(catalogManager)
        this.uriResolvers = uriResolvers
        this.logger = logger
    }


    @Override
    String getResolvedEntity(String publicId, String systemId) {

        logger.debug 'Resolving publicId [{}], systemId [{}]', publicId, systemId

        def superResolvedEntity = super.getResolvedEntity(publicId, systemId)
        logger.debug 'Parent resolver has resolved publicId [{}], systemId[{}] to [{}]',
                publicId, systemId, superResolvedEntity

        if (superResolvedEntity != null) {
            systemId = superResolvedEntity
        }

        if (systemId == null) {
            return null
        }

        def uri = new URI(systemId)

        for (;;) {
            def uriResolver = uriResolvers.get(uri.scheme)

            if (!uriResolver) {
                break
            }

            try {
                def resolvedUri = uriResolver.resolve(uri)

                if (resolvedUri != null) {
                    uri = resolvedUri
                } else {
                    break
                }

            } catch (Exception ex) {
                logger.warn('Resolver for the "{}" scheme could not resolve system ID: [{}]',
                        uri.scheme, systemId, ex)
                return superResolvedEntity
            }
        }

        return uri.toString()
    }
}
