package org.unbrokendome.gradle.plugins.xjc.testutil.assertions

import assertk.Assert
import assertk.all
import assertk.assertions.isFile
import assertk.assertions.support.expected
import assertk.assertions.support.show
import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipFile


fun Assert<File>.withJarFile(block: Assert<JarFile>.() -> Unit) {
    isFile()
    given { actual ->
        JarFile(actual).use { jarFile ->
            assertThat(jarFile, "jar:$name").all(block)
        }
    }
}


fun Assert<ZipFile>.containsEntry(name: String) = given { actual ->
    if (actual.getEntry(name) == null) {
        val actualEntryNames = actual.entries().toList().map { it.name }
        expected(
            "to have an entry ${show(name)}, but was: ${show(actualEntryNames)}",
            actual = actualEntryNames
        )
    }
}


fun Assert<ZipFile>.containsEntries(vararg names: String) = all {
    for (name in names) {
        containsEntry(name)
    }
}


fun Assert<ZipFile>.doesNotContainEntry(name: String) = given { actual ->
    if (actual.getEntry(name) != null) {
        val actualEntryNames = actual.entries().toList().map { it.name }
        expected(
            "to have no entry ${show(name)}, but was: ${show(actualEntryNames)}",
            actual = actualEntryNames
        )
    }
}


fun Assert<ZipFile>.doesNotContainEntries(vararg names: String) = all {
    for (name in names) {
        doesNotContainEntry(name)
    }
}
