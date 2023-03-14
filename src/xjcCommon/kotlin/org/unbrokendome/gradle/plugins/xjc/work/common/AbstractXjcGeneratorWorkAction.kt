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
import org.unbrokendome.gradle.plugins.xjc.resolver.ReflectionHelper
import org.xml.sax.InputSource
import java.lang.reflect.InvocationTargetException
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale


abstract class AbstractXjcGeneratorWorkAction : WorkAction<XjcGeneratorWorkParameters> {

    private val logger = Logging.getLogger(javaClass)


    protected open fun getContextClassLoaderHolder(): IContextClassLoaderHolder = ContextClassLoaderHolder()

    protected open fun getOptionsAccessor(options: Options): IOptionsAccessor = OptionsAccessor(options)

    override fun execute() {

        parameters.targetDir.get().asFile.mkdirs()

        val options = setupBuildOptions()

        val contextClassLoaderHolder = getContextClassLoaderHolder()

        try {
            contextClassLoaderHolder.setup(options)

            buildOptions(options)

            val docLocale = parameters.docLocale.orNull
            if (docLocale != null) {
                withDefaultLocale(Locale.forLanguageTag(docLocale)) {
                    doExecute(options)
                }
            } else {
                doExecute(options)
            }
        } finally {
            contextClassLoaderHolder.restore()
        }
    }

    protected open fun checkApiInClassPath() {
        val javax_className = "javax.xml.bind.JAXBContextFactory"
        val jakarta_className = "jakarta.xml.bind.JAXBContextFactory"

        var foundJavax = false
        var foundJakarta = false
        try {
            Class.forName(javax_className)
            foundJavax = true
        } catch (_: ClassNotFoundException) {
        }
        try {
            Class.forName(jakarta_className)
            foundJakarta = true
        } catch (_: ClassNotFoundException) {
        }

        if(!foundJavax && !foundJakarta)
            logger.warn("Unable to locate expected class {} or {} on XJC visible ClassPath, maybe you have used one or more xjcTool terms and need to also include *.bind-api in an additional xjcTool option.",
                javax_className, jakarta_className)

        if(foundJavax && foundJakarta)
            logger.warn("Found both {} and {} on XJC visible ClassPath, usually you only need one of these.",
                javax_className, jakarta_className)
    }

    protected open fun doExecute(options: Options) {

        val listener = LoggingXjcListener()
        val errorReceiver = ErrorReceiverFilter(listener)

        checkApiInClassPath()

        try {
            val jCodeModel = JCodeModel()

            addClassNameReplacer(options.target, jCodeModel)

            val model = ModelLoader.load(options, jCodeModel, errorReceiver)
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
        } finally {
            ReflectionHelper.closeAll()
        }
    }

    private fun reflectiveInvoke_JCodeModel_addClassNameReplacer(o: Any, c1: String?, c2: String?): Boolean {
        try {
            // JCodeModel#addClassNameReplacer(String,String)
            val m = o.javaClass.getMethod("addClassNameReplacer", String::class.java, String::class.java)
            m.invoke(o, c1, c2)
            return true
        } catch (_: NoSuchMethodException) {
        } catch (_: IllegalAccessException) {
        } catch (_: IllegalArgumentException) {
        } catch (_: InvocationTargetException) {
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
        return false
    }

    private fun addClassNameReplacer(target: SpecVersion, jCodeModel: JCodeModel) {
        // runtime lookup of V3_0 instance
        val specVersion30 = SpecVersion.parse("3.0")

        if(specVersion30 == null)
            return  // no support for v3.0 in XJC implementation

        if(target.ordinal >= specVersion30.ordinal)
            return  // targeting 3.0 or newer, nothing to do here

        // if target version is < 3.0

        // if implementation is: com.sun.xml.bind.jaxb-xjc  or has class ?
        //   we don't really check this, this allows the feature to work on 3rd party XJC
        //   implementations where the addClassNameReplacer(String,String) method is available
        // if implementation version is >= 3.0 or simply has the method ?
        //   the method was only introduced since 3.0 so it doesn't matter if we can't
        //   find it on older versions.
        // if a new version of XJC 2.x were to came along with the method available in the
        //   future adding these replacement still doesn't break anything while targeting < 3.0
        val JAKARTA = "jakarta.xml.bind"            // Regex.escape() breaks it
        val JAVAX = "javax.xml.bind"
        val JAXB_CORE = "org.glassfish.jaxb.core"   // Regex.escape() breaks it
        val BIND = "com.sun.xml.bind"

        // The JCodeModel#addClassNameReplacer(String c1,String c2) method is documented
        //  as taking a string Regex in c1.  You would expect to quote-meta on the
        //  full-stop characters as the correct c1 value.  But it also uses the c1
        //  value in a String#startsWith(c1) check before doing a String#replaceAll(c1, c2).

        reflectiveInvoke_JCodeModel_addClassNameReplacer(jCodeModel, JAKARTA, JAVAX)
        reflectiveInvoke_JCodeModel_addClassNameReplacer(jCodeModel, JAXB_CORE, BIND)
    }

    protected open fun setupBuildOptions() = Options().apply {
        parameters.pluginClasspath.forEach { classpathEntry ->
            classpaths.add(classpathEntry.toURI().toURL())
        }
    }

    protected open fun buildOptions(options: Options) = options.apply {
        val optionsAccessor = getOptionsAccessor(options)


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
            optionsAccessor.setEncoding(it)   // encoding = it
        }

        parameters.episodeTargetFile.orNull?.asFile?.let { episodeTargetFile ->
            episodeTargetFile.parentFile.mkdirs()
            parseArguments(arrayOf("-episode", episodeTargetFile.absolutePath))
        }

        if (logger.isInfoEnabled) {
            logger.info(
                "XJC options:\n{}", dumpOptions(optionsAccessor).prependIndent("  ")
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


    private fun Options.dumpOptions(optionsAccessor: IOptionsAccessor) = buildString {
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
        if(optionsAccessor.hasEncoding())
            append("encoding: ").appendln(optionsAccessor.getEncoding())
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
