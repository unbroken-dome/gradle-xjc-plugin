package org.unbrokendome.gradle.plugins.xjc.work.common

import com.sun.codemodel.JCodeModel
import com.sun.tools.xjc.ModelLoader
import com.sun.tools.xjc.Options
import com.sun.tools.xjc.api.SpecVersion
import com.sun.tools.xjc.util.ErrorReceiverFilter
import org.apache.xml.resolver.CatalogManager
import org.apache.xml.resolver.tools.CatalogResolver
import org.gradle.api.logging.Logging
import org.gradle.workers.WorkAction
import org.unbrokendome.gradle.plugins.xjc.internal.XjcGeneratorFlags
import org.unbrokendome.gradle.plugins.xjc.internal.XjcGeneratorWorkParameters
import org.unbrokendome.gradle.plugins.xjc.resolver.ClasspathUriResolver
import org.unbrokendome.gradle.plugins.xjc.resolver.ExtensibleCatalogResolver
import org.unbrokendome.gradle.plugins.xjc.resolver.MavenUriResolver
import org.xml.sax.InputSource
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale


abstract class AbstractXjcGeneratorWorkAction : WorkAction<XjcGeneratorWorkParameters> {

    private val logger = Logging.getLogger(javaClass)


    override fun execute() {

        parameters.targetDir.get().asFile.mkdirs()

        val options = buildOptions()

        val docLocale = parameters.docLocale.orNull
        if (docLocale != null) {
            withDefaultLocale(Locale.forLanguageTag(docLocale)) {
                doExecute(options)
            }
        } else {
            doExecute(options)
        }
    }


    protected open fun doExecute(options: Options) {

        val listener = LoggingXjcListener()
        val errorReceiver = ErrorReceiverFilter(listener)

        val model = ModelLoader.load(options, JCodeModel(), errorReceiver)
            ?: throw Exception("Parse failed")

        val outline = model.generateCode(options, errorReceiver)
            ?: throw Exception("Code generation failed")

        listener.compiled(outline)

        val codeWriter = options.createCodeWriter()
            .let { cw ->
                if (!options.quiet) {
                    ProgressCodeWriter(cw, listener, model.codeModel.countArtifacts())
                } else cw
            }

        model.codeModel.build(codeWriter)
    }


    protected open fun buildOptions() = Options().apply {

        parameters.pluginClasspath.forEach { classpathEntry ->
            classpaths.add(classpathEntry.toURI().toURL())
        }

        // Set up the classloader containing the plugin classpath. This should happen before any call
        // to parseArgument() or parseArguments() because it might trigger the resolution of plugins
        // (which are then cached)
        val contextClassLoader = Thread.currentThread().contextClassLoader
        val userClassLoader = getUserClassLoader(contextClassLoader)
        Thread.currentThread().contextClassLoader = userClassLoader

        target = parameters.target
            .map { SpecVersion.parse(it) }
            .getOrElse(SpecVersion.LATEST)

        targetDir = parameters.targetDir.get().asFile

        if (!parameters.catalogFiles.isEmpty) {
            val catalogResolver = createCatalogResolver()
            parameters.catalogFiles.forEach { catalogFile ->
                logger.debug("Adding XJC catalog file: {}", catalogFile)
                catalogResolver.catalog.parseCatalog(catalogFile.absolutePath)
            }
            entityResolver = catalogResolver
        }

        parameters.sourceFiles.forEach { sourceFile ->
            addGrammar(sourceFile)
        }

        parameters.urlSources.forEach { urlSourceFile ->
            urlSourceFile.forEachLine { line ->
                if (line.isNotBlank()) {
                    val url = line.trim()
                    try {
                        URI(url)
                    } catch (ex: URISyntaxException) {
                        throw IllegalArgumentException("Invalid URL: $url", ex)
                    }
                    addGrammar(InputSource(url))
                }
            }
        }

        parameters.bindingFiles.forEach { bindingFile ->
            addBindFile(bindingFile)
        }

        parameters.episodes.forEach { episodeFile ->
            scanEpisodeFile(episodeFile)
        }

        parameters.flags.getOrElse(emptySet()).let { flags ->
            strictCheck = XjcGeneratorFlags.STRICT_CHECK in flags
            packageLevelAnnotations = XjcGeneratorFlags.PACKAGE_LEVEL_ANNOTATIONS in flags
            noFileHeader = XjcGeneratorFlags.NO_FILE_HEADER in flags
            enableIntrospection = XjcGeneratorFlags.ENABLE_INTROSPECTION in flags
            contentForWildcard = XjcGeneratorFlags.CONTENT_FOR_WILDCARD in flags
            readOnly = XjcGeneratorFlags.READ_ONLY in flags

            // extension mode should be enabled automatically if we are using episodes or plugins
            if (XjcGeneratorFlags.EXTENSION in flags ||
                !parameters.episodes.isEmpty ||
                parameters.extraArgs.orNull.orEmpty().any { it.startsWith("-X") }) {
                compatibilityMode = Options.EXTENSION
            }
        }

        parameters.targetPackage.orNull?.let {
            defaultPackage = it
        }

        parameters.encoding.orNull?.let {
            encoding = it
        }

        parameters.episodeTargetFile.orNull?.asFile?.let { episodeTargetFile ->
            episodeTargetFile.parentFile.mkdirs()
            parseArguments(arrayOf("-episode", episodeTargetFile.absolutePath))
        }

        if (logger.isInfoEnabled) {
            logger.info(
                "XJC options:\n{}", dumpOptions().prependIndent("  ")
            )
            logger.info("XJC extra args: {}", parameters.extraArgs.get())

            logger.info(
                "XJC catalog resolution classpath:\n{}",
                parameters.catalogResolvedArtifacts.get().joinToString(separator = "\n") { "  - $it" }
            )
        }

        parameters.extraArgs.orNull?.let {
            parseArguments(it.toTypedArray())
        }
    }


    private fun Options.dumpOptions() = buildString {
        append("targetVersion: ").appendln(target)

        appendln("grammars:")
        for (grammar in grammars) {
            append("  - ").appendln(grammar.systemId)
        }
        if (bindFiles.isNotEmpty()) {
            appendln("bindFiles:")
            for (bindingFile in bindFiles) {
                append("  - ").appendln(bindingFile.systemId)
            }
        }
        if (classpaths.isNotEmpty()) {
            appendln("classpaths:")
            for (classpath in classpaths) {
                append("  - ").appendln(classpath)
            }
        }
        append("strictCheck: ").appendln(strictCheck)
        append("packageLevelAnnotations: ").appendln(packageLevelAnnotations)
        append("noFileHeader: ").appendln(noFileHeader)
        append("enableIntrospection: ").appendln(enableIntrospection)
        append("contentForWildcard: ").appendln(contentForWildcard)
        append("compatibilityMode: ").appendln(if (isExtensionMode) "EXTENSION" else "STRICT")
        append("defaultPackage: ").appendln(defaultPackage)
        append("defaultPackage2: ").appendln(defaultPackage2)
        append("encoding: ").appendln(encoding)
        appendln("plugins:")
        for (plugin in allPlugins) {
            append("  - ").append(plugin.javaClass.name)
                .append(" (option: ").append(plugin.optionName).appendln(")")
        }
    }


    private fun createCatalogResolver(): CatalogResolver {

        val catalogManager = CatalogManager().apply {
            ignoreMissingProperties = true
            useStaticCatalog = false
        }

        return ExtensibleCatalogResolver(
            catalogManager,
            mapOf(
                "maven" to MavenUriResolver(parameters.catalogResolvedArtifacts.get()),
                "classpath" to ClasspathUriResolver
            )
        )
    }
}
