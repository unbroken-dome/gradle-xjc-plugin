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
            "3.0" to "org.unbrokendome.gradle.plugins.xjc.work.xjc30.XjcGeneratorWorkAction"
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

        val toolManifest = toolClasspath.findManifests()
            .singleOrNull { manifest ->
                val bundleSymbolicName = manifest.mainAttributes[ManifestAttributes.BundleSymbolicName]
                bundleSymbolicName == "com.sun.xml.bind.jaxb-xjc"
            }
            ?: throw IllegalStateException("Could not find a suitable XJC implementation on the tool classpath")

        val xjcVersion = toolManifest.mainAttributes[ManifestAttributes.SpecificationVersion]
        var resolvedWorkActionClassName = WorkActionClassNamesByVersion[xjcVersion]
        if(resolvedWorkActionClassName == null) {
            resolvedWorkActionClassName = WorkActionClassNamesByVersion[HIGHEST_SUPPORTED_VERSION]
                                        ?: throw IllegalStateException("Cannot handle XJC version: $xjcVersion")
            // This creates a strategy to always warn the user and proceed with best effort
            // We should force the user to set an option to active this best effort, forcing a configuration decision
            // xjc.xjcVersionUnsupportedStrategy ?
            logger.warn("Version {} XJC is unsupported, using strategy for highest supported version {}", xjcVersion, HIGHEST_SUPPORTED_VERSION)
        }
        val workActionClassName = resolvedWorkActionClassName

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
