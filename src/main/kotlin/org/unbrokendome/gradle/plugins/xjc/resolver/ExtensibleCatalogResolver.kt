package org.unbrokendome.gradle.plugins.xjc.resolver

import org.apache.xml.resolver.CatalogManager
import org.apache.xml.resolver.tools.CatalogResolver
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.xjc.resolver.ReflectionHelper.Companion.reflectiveInvoke_URLConnection_getDefaultUseCaches
import org.unbrokendome.gradle.plugins.xjc.resolver.ReflectionHelper.Companion.reflectiveInvoke_URLConnection_setDefaultUseCaches
import org.xml.sax.InputSource
import java.net.URI


class ExtensibleCatalogResolver(
    catalogManager: CatalogManager,
    private val uriResolvers: Map<String, UriResolver>
) : CatalogResolver(catalogManager) {

    companion object {
        private val logger = LoggerFactory.getLogger(ExtensibleCatalogResolver::class.java)
    }

    override fun resolveEntity(publicId: String?, systemId: String?): InputSource? {
        val JAR = "jar"
        // val useCaches = URLConnection.getDefaultUseCaches(JAR)
        val useCaches = reflectiveInvoke_URLConnection_getDefaultUseCaches(JAR)
        try {
            reflectiveInvoke_URLConnection_setDefaultUseCaches(JAR, false)
            // URLConnection.setDefaultUseCaches(JAR, false)

            logger.debug("resolveEntity({}, {}): lookup", publicId, systemId)

            // Why are we intercepting this?  Well the classpath JAR loader is based on URLConnection
            // and this has a concept of cacheable entities.  So it attempts to maintain a shared and
            // open file handle to the containing JAR file even when the last resource InputStream
            // needing it has been closed.
            //
            // The Apache xml-resolver:1.2 implementation always returns an open InputStream for a
            // successful use of #resolveEnntity(String,String) as per
            // org.apache.xml.resolver.tools.CatalogResolver#L211
            //
            // This causes a problem, as even though Gradle and this plugin manage the XJC classloader
            // to isolate even when that classloader is closed the JVM still maintains an open file
            // handle, which is a problem for using Gradle daemon in a long-running way from IDE in
            // Windows where JAR locking is a headache.
            val inputSource = super.resolveEntity(publicId, systemId)

            if (inputSource?.byteStream != null)
                ReflectionHelper.addInputStream(inputSource.byteStream)
            // inputSource.characterStream // this exists as well but not used

            return inputSource
        } finally {
            // URLConnection.setDefaultUseCaches(JAR, useCaches)
            if(useCaches != null)
                reflectiveInvoke_URLConnection_setDefaultUseCaches(JAR, useCaches)
        }
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
