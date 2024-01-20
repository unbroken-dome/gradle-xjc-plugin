package org.unbrokendome.gradle.plugins.xjc.internal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkParameters


interface XjcGeneratorWorkParameters : WorkParameters {

    val target: Property<String>
    val targetDir: DirectoryProperty
    val sourceFiles: ConfigurableFileCollection
    val urlSources: ConfigurableFileCollection
    val bindingFiles: ConfigurableFileCollection
    val pluginClasspath: ConfigurableFileCollection
    val catalogFiles: ConfigurableFileCollection
    val catalogResolvedArtifacts: ListProperty<SerializableResolvedArtifact>
    val episodes: ConfigurableFileCollection
    val targetPackage: Property<String>
    val encoding: Property<String>
    val docLocale: Property<String>
    val episodeTargetFile: RegularFileProperty
    val extraArgs: ListProperty<String>
    val flags: SetProperty<XjcGeneratorFlags>
}
