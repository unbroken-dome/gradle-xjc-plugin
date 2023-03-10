package org.unbrokendome.gradle.plugins.xjc

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.xjc.internal.providerFromProjectProperty


/**
 * Contains global configuration for the [XjcPlugin]. It is added to the project when the plugin is applied.
 */
interface XjcExtension : XjcGeneratorOptions {

    companion object {
        const val DEFAULT_XJC_VERSION = "2.3"
        const val DEFAULT_SRC_DIR_NAME = "schema"
        const val DEFAULT_XJC_VERSION_UNSUPPORTED_STRATEGY = "default"
    }

    /**
     * The version of the XJC _tool_ to use.
     *
     * This will influence the version of the XJC compiler that is used, but not (directly) the parameters
     * that are passed to it. Valid values are `2.1`, `2.2`, `2.3`, `2.4`, `3.0` and `4.0`.
     *
     * The value of this property only influences the set of
     * [defaultDependencies][org.gradle.api.artifacts.Configuration.defaultDependencies] on the global `xjcTool`
     * configuration. This means that if you added any dependencies to that configuration, this property will have
     * no effect.
     *
     * The default is currently `2.3`, but this may change in future versions of the plugin.
     */
    val xjcVersion: Property<String>

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
     *   `legacy-latest` to use the latest known legacy strategy as used for 2.1 XJC tools.
     *   `2.3` would force a specific strategy of a specific XJC tool version.
     *     Any string value also valid for `xjcVersion` is allowed, `2.3` is an example of one of those values.
     *
     * Default is `default`.
     */
    val xjcVersionUnsupportedStrategy: Property<String>

    /**
     * The conventional name of the XJC source directories.
     *
     * The default is `"schema"`.
     *
     * For every source set, the corresponding XJC generator will look for schemas (`*.xsd`), binding files (`*.xjb`)
     * and URL files (`*.url`) in a directory `src/<source-set-name>/<srcDirName>`. For example, if this property is
     * set to `"schema"`, XJC generation for the `main` source set will use sources from `src/main/schema`.
     *
     * Change this property to change the conventional name for _all_ source sets.
     */
    val srcDirName: Property<String>

    /**
     * Additional arguments to be passed to the XJC compiler.
     *
     * This can be used, for example, to pass arguments controlling the behavior of plugins.
     */
    val extraArgs: ListProperty<String>
}


private fun XjcExtension.setFromProjectProperties(project: Project) = apply {
    xjcVersion.set(
        project.providerFromProjectProperty("xjc.xjcVersion", XjcExtension.DEFAULT_XJC_VERSION)
    )
    xjcVersionUnsupportedStrategy.set(
        project.providerFromProjectProperty("xjc.xjcVersionUnsupportedStrategy", XjcExtension.DEFAULT_XJC_VERSION_UNSUPPORTED_STRATEGY)
    )
    srcDirName.set(
        project.providerFromProjectProperty("xjc.srcDirName", XjcExtension.DEFAULT_SRC_DIR_NAME)
    )
    (this as XjcGeneratorOptions).setFromProjectProperties(project)
}


/**
 * Create a managed [XjcExtension] instance.
 */
internal fun Project.createXjcExtension(): XjcExtension =
    objects.newInstance(XjcExtension::class.java)
        .setFromProjectProperties(project)
