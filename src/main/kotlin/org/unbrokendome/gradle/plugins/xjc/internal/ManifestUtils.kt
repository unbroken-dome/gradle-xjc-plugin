package org.unbrokendome.gradle.plugins.xjc.internal

import org.gradle.api.file.FileCollection
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest


internal object ManifestAttributes {

    val MainClass = Attributes.Name("Main-Class")
    val BundleSymbolicName = Attributes.Name("Bundle-SymbolicName")
    val SpecificationVersion = Attributes.Name("Specification-Version")
    val ExtensionName = Attributes.Name("Extension-Name")
}


internal fun FileCollection.findManifests(): Sequence<Manifest> =
    asSequence()
        .mapNotNull { toolJar ->
            JarFile(toolJar).use { jarFile ->
                jarFile.manifest
            }
        }
