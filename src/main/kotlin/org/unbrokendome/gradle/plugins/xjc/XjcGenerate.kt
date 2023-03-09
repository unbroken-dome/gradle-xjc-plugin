package org.unbrokendome.gradle.plugins.xjc

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkerExecutor
import org.unbrokendome.gradle.plugins.xjc.internal.ManifestAttributes
import org.unbrokendome.gradle.plugins.xjc.internal.SerializableResolvedArtifact
import org.unbrokendome.gradle.plugins.xjc.internal.XjcGeneratorWorkParameters
import org.unbrokendome.gradle.plugins.xjc.internal.findManifests
import java.io.File
import javax.inject.Inject


/**
 * Invokes XJC to generate code from schemas.
 */
@CacheableTask
@Suppress("LeakingThis")
abstract class XjcGenerate
@Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask(), XjcGeneratorOptions {

    companion object {

        /**
         * Used to select the correct WorkAction for a given JAXB spec version.
         */
        private val WorkActionClassNamesByVersion = mapOf(
            "2.2" to "org.unbrokendome.gradle.plugins.xjc.work.xjc22.XjcGeneratorWorkAction",
            "2.3" to "org.unbrokendome.gradle.plugins.xjc.work.xjc23.XjcGeneratorWorkAction",
            "2.4" to "org.unbrokendome.gradle.plugins.xjc.work.xjc24.XjcGeneratorWorkAction",
            "3.0" to "org.unbrokendome.gradle.plugins.xjc.work.xjc30.XjcGeneratorWorkAction",
            "4.0" to "org.unbrokendome.gradle.plugins.xjc.work.xjc40.XjcGeneratorWorkAction"
        )
        private val HIGHEST_SUPPORTED_VERSION = WorkActionClassNamesByVersion.keys.max()
    }


    init {
        group = "code generation"
        description = "Generates Java classes from XML schema using the XJC binding compiler"
    }


    /**
     * The schema files.
     */
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val source: ConfigurableFileCollection


    /**
     * The binding customization files.
     *
     * Corresponds to the `-b` CLI option.
     */
    @get:[InputFiles Optional PathSensitive(PathSensitivity.RELATIVE)]
    abstract val bindingFiles: ConfigurableFileCollection


    /**
     * The URL sources. These should be text files that contain a URL referencing another schema on each line.
     */
    @get:[InputFiles Optional PathSensitive(PathSensitivity.RELATIVE)]
    abstract val urlSources: ConfigurableFileCollection


    /**
     * The catalog files.
     *
     * Corresponds to the `-catalog` CLI option.
     */
    @get:[InputFiles Optional PathSensitive(PathSensitivity.RELATIVE)]
    abstract val catalogs: ConfigurableFileCollection


    /**
     * Episodes to be imported into this XJC generation step.
     */
    @get:[InputFiles Optional Classpath]
    abstract val episodes: ConfigurableFileCollection


    /**
     * The classpath containing the XJC tool itself.
     */
    @get:[InputFiles Classpath]
    abstract val toolClasspath: ConfigurableFileCollection


    /**
     * The classpath containing XJC plugins.
     */
    @get:[InputFiles Optional Classpath]
    abstract val pluginClasspath: ConfigurableFileCollection


    /**
     * A [Configuration] to be used for catalog resolution with the `maven:` URI scheme.
     */
    @get:[InputFiles Optional Classpath]
    abstract val catalogResolutionClasspath: Property<Configuration>


    /**
     * Additional arguments to be passed to XJC for this source set.
     *
     * This property will be initialized with any [extraArgs][XjcExtension.extraArgs] specified in the global `xjc`
     * block _and_ any [xjcExtraArgs][XjcSourceSetConvention.xjcExtraArgs] specified in the source set. If you would
     * like to preserve those, make sure to add to this list instead of replacing its contents.
     */
    @get:[Input Optional]
    abstract val extraArgs: ListProperty<String>


    /**
     * The target package for XJC.
     *
     * When you specify a target package with this command-line option, it overrides any binding customization for the
     * package name and the default package name algorithm defined in the specification.
     *
     * Corresponds to the `-p` command line option.
     */
    @get:[Input Optional]
    abstract val targetPackage: Property<String>


    /**
     * The output directory where generated files will be placed.
     *
     * Corresponds to the `-d` CLI option.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty


    /**
     * If `true`, instructs XJC to generate an episode file at `META-INF/sun-jaxb.episode`.
     *
     * The default is `false`.
     */
    @get:Input
    abstract val generateEpisode: Property<Boolean>


    /**
     * The base output directory for generated episodes. Only relevant if [generateEpisode] is set to `true`.
     *
     * Default is to use the same directory as [outputDirectory].
     */
    @get:OutputDirectory
    abstract val episodeOutputDirectory: DirectoryProperty


    /**
     * This setting exists to support newer versions of the XJC tool than the gradle plugin has been
     *  tested with and officially supports as listed in the `xjcVersion` setting.
     *
     * This setting exists because doing this silently is probably not what all users would want by default.
     * Without this setting configured the plugin will error informing you the XJC tool is newer than that
     *  officially supported by the plugin and the existence of this setting provides a strategy that may
     *  be able to resolve the build failure situation.
     *
     * This forces the documentation to be read and an administrative decision to be made for your
     *  project concerning any risks for using unsupported XJC tooling versions in this way.
     * If this setting is configured to something other than `default` you are advised to manually check
     *  the generated output conforms to your expectations.
     *
     * If you wish the plugin to continue configure this setting to `auto-resolve`.
     *
     * Possible values might be:
     *   `default` This will auto-detect XJC tool version and only allow supported versions to proceed.
     *   `auto-resolve` This will behave as `default` first, if `default` fails then proceed using `latest`.
     *     This strategy is anticipated to be the action most users want in the situation.
     *   `latest` to use the strategy for the latest supported version.  As listed in `xjcVersion`.
     *   `2.3` would force a specific strategy of a specific XJC tool version.
     *     Any string value also valid for `xjcVersion` is allowed, `2.3` is an example of one of those values.
     *
     * Default is `default`.
     */
    @get:[Input Optional]
    abstract val xjcVersionUnsupportedStrategy: Property<String>


    init {
        setFromProjectProperties(project)
        generateEpisode.convention(false)
        episodeOutputDirectory.convention(outputDirectory)
    }


    @TaskAction
    fun generate() {

        logger.info("Cleaning the output directory")

        // Start by always cleaning the output directory first. JAXB is not incremental and doesn't remove
        // leftover files that would not be generated anymore.
        val anythingDeleted = cleanOutputDirs()

        // If we don't have any schemas or URL sources, we don't need to call JAXB as it wouldn't output anything.
        if (source.isEmpty && urlSources.isEmpty) {
            logger.info("{} has no source schemas", this)
            didWork = anythingDeleted
            return
        }

        val workActionClassName = resolveXjcVersion()

        @Suppress("UNCHECKED_CAST")
        val workActionClass = Class.forName(workActionClassName)
            .asSubclass(WorkAction::class.java) as Class<out WorkAction<XjcGeneratorWorkParameters>>

        workerExecutor
            .classLoaderIsolation {
                it.classpath.from(toolClasspath)
            }
            .submit(workActionClass) { parameters ->
                fillParameters(parameters)
            }
    }

    private fun resolveXjcVersion(): String {
        val toolManifest = toolClasspath.findManifests()
            .singleOrNull { manifest ->
                val bundleSymbolicName = manifest.mainAttributes[ManifestAttributes.BundleSymbolicName]
                bundleSymbolicName == "com.sun.xml.bind.jaxb-xjc"
            }
            ?: throw IllegalStateException("Could not find a suitable XJC implementation on the tool classpath, expecting: com.sun.xml.bind.jaxb-xjc")

        val xjcVersionSpecificationVersion = toolManifest.mainAttributes[ManifestAttributes.SpecificationVersion]?.toString()
        // Although this setting is called xjcVersionUnsupportedStrategy externally it is actually internally xjcVersionStrategy,
        //  it is named as it is to better reflect to the target audience the correct level of concern when configuring it.
        val xjcVersionStrategy = xjcVersionUnsupportedStrategy.get()

        val isDefault = "default".equals(xjcVersionStrategy, true)
        val isAutoResolve = "auto-resolve".equals(xjcVersionStrategy, true)

        var xjcVersion: String?
        if("latest".equals(xjcVersionStrategy, true))
            xjcVersion = HIGHEST_SUPPORTED_VERSION
        else if(!isDefault && !isAutoResolve && xjcVersionStrategy.isNotEmpty())
            xjcVersion = xjcVersionStrategy
        else    // "default" || "auto-resolve" || empty || null
            xjcVersion = xjcVersionSpecificationVersion

        val isSupported = WorkActionClassNamesByVersion[xjcVersionSpecificationVersion] != null
        var resolvedWorkActionClassName = WorkActionClassNamesByVersion[xjcVersion]

        if(resolvedWorkActionClassName == null && isAutoResolve)
            resolvedWorkActionClassName = WorkActionClassNamesByVersion[HIGHEST_SUPPORTED_VERSION]

        if(resolvedWorkActionClassName == null) {
            val word = if(isDefault) "found" else "requested"
            val msg = "Unsupported XJC version $word $xjcVersion consult documentation for setting " +
                "xjcVersionUnsupportedStrategy='auto-resolve' to provide the plugin a strategy to continue"
            throw IllegalStateException(msg)
        }

        if(!isDefault) {
            logger.warn(
                "xjcVersionUnsupportedStrategy is set {}, XJC tool version found {} this is {}supported, " +
                        "using strategy for XJC tool version {}",
                xjcVersionStrategy, xjcVersionSpecificationVersion,
                (if (isSupported) "" else "un"),
                xjcVersion
            )
        }

        return resolvedWorkActionClassName
    }

    private fun cleanOutputDirs(): Boolean {
        val outputCleaned = outputDirectory.get().asFile.deleteAllContents()
        val episodeOutputCleaned = episodeOutputDirectory.get().asFile.deleteAllContents()

        return outputCleaned || episodeOutputCleaned
    }


    private fun File.deleteAllContents(): Boolean =
        walkBottomUp()
            .fold(false) { anythingDeleted, file ->
                file.delete() || anythingDeleted
            }


    private fun fillParameters(parameters: XjcGeneratorWorkParameters) {
        parameters.target.set(targetVersion)
        parameters.targetDir.set(outputDirectory)
        parameters.sourceFiles.setFrom(source)
        parameters.urlSources.setFrom(urlSources)
        parameters.bindingFiles.setFrom(bindingFiles)
        parameters.pluginClasspath.setFrom(pluginClasspath)
        parameters.catalogFiles.setFrom(catalogs)
        parameters.catalogResolvedArtifacts.set(
            catalogResolutionClasspath.map { configuration ->
                configuration.resolvedConfiguration.lenientConfiguration.artifacts
                    .map { SerializableResolvedArtifact(it) }
            }
        )
        parameters.episodes.setFrom(episodes)
        parameters.targetPackage.set(targetPackage)
        parameters.encoding.set(encoding)
        parameters.docLocale.set(docLocale)

        if (generateEpisode.get()) {
            parameters.episodeTargetFile.set(
                episodeOutputDirectory.file("META-INF/sun-jaxb.episode")
            )
        }

        parameters.extraArgs.set(extraArgs)
        parameters.flags.set(buildFlags())
    }
}
