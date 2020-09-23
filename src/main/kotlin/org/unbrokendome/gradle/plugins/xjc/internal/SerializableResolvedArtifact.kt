package org.unbrokendome.gradle.plugins.xjc.internal

import org.gradle.api.artifacts.ResolvedArtifact
import java.io.File
import java.io.Serializable


data class SerializableResolvedArtifact(
    val file: File,
    val group: String?,
    val name: String?,
    val version: String?,
    val extension: String?,
    val classifier: String?
) : Serializable {

    constructor(artifact: ResolvedArtifact) : this(
        file = artifact.file,
        group = artifact.moduleVersion.id.group,
        name = artifact.moduleVersion.id.name,
        version = artifact.moduleVersion.id.version,
        extension = artifact.file.extension,
        classifier = artifact.classifier
    )


    override fun toString(): String =
        "${group ?: ""}:${name ?: ""}:${extension ?: ""}:${classifier ?: ""}:${version ?: ""} -> $file"
}

