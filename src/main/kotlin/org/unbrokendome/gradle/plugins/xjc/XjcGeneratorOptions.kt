package org.unbrokendome.gradle.plugins.xjc

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.unbrokendome.gradle.plugins.xjc.internal.XjcGeneratorFlags
import org.unbrokendome.gradle.plugins.xjc.internal.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.xjc.internal.providerFromProjectProperty
import java.util.EnumSet


/**
 * Flag-style options for influencing the XJC generator behavior.
 */
interface XjcGeneratorOptions {

    /**
     * The version of the JAXB specification to target.
     *
     * If not set, defaults to the latest version known by the XJC tool.
     */
    @get:[Input Optional]
    val targetVersion: Property<String>

    /**
     * The encoding to use for generated files.
     *
     * Corresponds to the `-encoding` CLI option.
     */
    @get:Input
    val encoding: Property<String>

    /**
     * The documentation locale to be used when running XJC. This may influence the language of documentation
     * comments in XJC-generated files.
     *
     * Note: XJC does not support a parameter for setting the locale. If this property is set, the worker will set
     * the JVM's default locale to the given one before calling XJC, and switch it back afterwards.
     */
    @get:[Input Optional]
    val docLocale: Property<String>

    /**
     * If `true`, perform strict schema validation.
     *
     * The default is `true`.
     *
     * Corresponds to the `-nv` CLI parameter (inverted).
     */
    @get:Input
    val strictCheck: Property<Boolean>

    /**
     * If `true`, generate package-level annotations into `package-info.java` files. If `false`, those annotations
     * are internalized into the other generated classes.
     *
     * The default is `true`.
     *
     * Corresponds to the `-npa` CLI parameter (inverted).
     */
    @get:Input
    val packageLevelAnnotations: Property<Boolean>

    /**
     * If `true`, suppresses the generation of a file header comment that includes some note and time stamp.
     *
     * The default is `true`.
     *
     * Corresponds to the `-no-header` CLI parameter.
     */
    @get:Input
    val noFileHeader: Property<Boolean>

    /**
     * If `true`, fix getter/setter generation to match the Bean introspection API.
     *
     * The default is `false`.
     */
    @get:Input
    val enableIntrospection: Property<Boolean>

    /**
     * If `true`, generates content property for types with multiple `xs:any` derived elements (which is supposed to
     * be correct behavior).
     *
     * The default is `false`.
     *
     * Corresponds to the `-contentForWildcard` CLI parameter.
     */
    @get:Input
    val contentForWildcard: Property<Boolean>

    /**
     * If `true` write-protect the generated Java source files.
     *
     * The default is `false`.
     *
     * Corresponds to the `readOnly` CLI parameter.
     */
    @get:Input
    val readOnly: Property<Boolean>

    /**
     * If `true`, enable JAXB vendor extensions.
     *
     * The default is `false`, but extension mode be enabled automatically when any `-X` argument is present in the
     * [XjcGenerate.extraArgs] list.
     *
     * Corresponds to the `-extension` CLI parameter.
     */
    @get:Input
    val extension: Property<Boolean>
}


internal fun XjcGeneratorOptions.setFromProjectProperties(project: Project) = apply {

    targetVersion.set(
        project.providerFromProjectProperty("xjc.targetVersion")
    )
    encoding.set(
        project.providerFromProjectProperty("xjc.encoding", "UTF-8")
    )
    docLocale.set(
        project.providerFromProjectProperty("xjc.docLocale")
    )
    strictCheck.set(
        project.booleanProviderFromProjectProperty("xjc.strictCheck", true)
    )
    packageLevelAnnotations.set(
        project.booleanProviderFromProjectProperty("xjc.packageLevelAnnotations", true)
    )
    noFileHeader.set(
        project.booleanProviderFromProjectProperty("xjc.noFileHeader", true)
    )
    enableIntrospection.set(
        project.booleanProviderFromProjectProperty("xjc.enableIntrospection", false)
    )
    contentForWildcard.set(
        project.booleanProviderFromProjectProperty("xjc.contentForWildcard", false)
    )
    readOnly.set(
        project.booleanProviderFromProjectProperty("xjc.readOnly", false)
    )
    extension.set(
        project.booleanProviderFromProjectProperty("xjc.extension", false)
    )
}


/**
 * Constructs a set of [XjcGeneratorFlags] from the current values of the properties.
 */
internal fun XjcGeneratorOptions.buildFlags() = buildFlagsSetFromProviders(
    XjcGeneratorFlags.STRICT_CHECK to strictCheck,
    XjcGeneratorFlags.PACKAGE_LEVEL_ANNOTATIONS to packageLevelAnnotations,
    XjcGeneratorFlags.NO_FILE_HEADER to noFileHeader,
    XjcGeneratorFlags.ENABLE_INTROSPECTION to enableIntrospection,
    XjcGeneratorFlags.CONTENT_FOR_WILDCARD to contentForWildcard,
    XjcGeneratorFlags.READ_ONLY to readOnly,
    XjcGeneratorFlags.EXTENSION to extension
)


private inline fun <reified F : Enum<F>> buildFlagsSetFromProviders(
    vararg flags: Pair<F, Provider<Boolean>>
): EnumSet<F> {
    val flagsMap = mapOf(*flags)
    return EnumSet.copyOf(
        enumValues<F>().filter { flagsMap[it]?.get() ?: false }
    )
}


/**
 * Sets the [XjcGeneratorOptions] properties from another [XjcGeneratorOptions] instance.
 */
internal fun XjcGeneratorOptions.setFrom(other: XjcGeneratorOptions) {

    targetVersion.set(other.targetVersion)
    encoding.set(other.encoding)
    docLocale.set(other.docLocale)

    strictCheck.set(other.strictCheck)
    packageLevelAnnotations.set(other.packageLevelAnnotations)
    noFileHeader.set(other.noFileHeader)
    enableIntrospection.set(other.enableIntrospection)
    contentForWildcard.set(other.contentForWildcard)
    readOnly.set(other.readOnly)
    extension.set(other.extension)
}
