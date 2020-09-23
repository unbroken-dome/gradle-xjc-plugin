package org.unbrokendome.gradle.plugins.xjc.resolver

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.specs.Spec


internal data class DependencyResource(
    private val group: String?,
    private val name: String?,
    private val version: String?,
    val classifier: String?,
    val ext: String?,
    val path: String?
) : ModuleVersionIdentifier, ModuleIdentifier, Spec<Dependency> {

    override fun getGroup(): String? = group


    override fun getName(): String? = name


    override fun getVersion(): String? = version


    override fun getModule(): ModuleIdentifier = this


    override fun isSatisfiedBy(dependency: Dependency): Boolean =
        (group == null || dependency.group == group) &&
                (name == null || dependency.name == name) &&
                (version == null || dependency.matchesVersion())


    private fun Dependency.matchesVersion() =
        if (this is ModuleVersionSelector) {
            matchesStrictly(this@DependencyResource)
        } else {
            version == this@DependencyResource.version
        }


    override fun toString() =
        listOf(
            group, name, ext, classifier, version, "!${path.orEmpty()}"
        ).joinToString(separator = ":") { it.orEmpty() }


    companion object {

        private val regex = Regex(
            """
            ^(?x)
            (?<group>[^:]*?):(?<name>[^:]*?)
            (?::(?<ext>[^:]*?)
                (?::(?<classifier>[^:]*?)
                    (?::(?<version>[^:]*?))?
                )?
            )?
            (?:!(?<path>.*))?
            $
            """.trimIndent()
        )


        fun parse(input: String): DependencyResource =
            requireNotNull(regex.matchEntire(input)) {
                "Error parsing dependency notation [$input]: does not match expected format"
            }.run {
                DependencyResource(
                    group = groups["group"]?.value.nullIfEmpty(),
                    name = groups["name"]?.value.nullIfEmpty(),
                    version = groups["version"]?.value.nullIfEmpty(),
                    classifier = groups["classifier"]?.value.nullIfEmpty(),
                    ext = groups["ext"]?.value.nullIfEmpty(),
                    path = groups["path"]?.value.nullIfEmpty()
                )
            }


        private fun String?.nullIfEmpty(): String? =
            this?.takeIf { it.isNotEmpty() }
    }
}
