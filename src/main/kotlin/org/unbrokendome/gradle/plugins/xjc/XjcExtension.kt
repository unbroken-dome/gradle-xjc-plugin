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
    }

    /**
     * The version of the XJC _tool_ to use.
     *
     * This will influence the version of the XJC compiler that is used, but not (directly) the parameters
     * that are passed to it. Valid values are `2.2`, `2.3` and `3.0`.
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
