package org.unbrokendome.gradle.plugins.xjc.testutil.assertions

import assertk.Assert
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider


fun <T : Any?> Assert<Provider<T>>.isPresent() = transform { actual ->
    actual.orNull ?: expected("${show(actual)} to have a value", actual = actual)
}


fun Assert<Provider<Boolean>>.isTrue() =
    isPresent().isTrue()


fun Assert<Provider<Boolean>>.isFalse() =
    isPresent().isFalse()


fun <T : Any?> Assert<Provider<T>>.hasValueEqualTo(value: T) =
    isPresent().isEqualTo(value)


fun Assert<Provider<RegularFile>>.fileValue() =
    isPresent()
        .prop("file") { it.asFile }


fun Assert<Provider<Directory>>.dirValue() =
    isPresent()
        .prop("directory") { it.asFile }


fun <K : Any, V : Any> Assert<Provider<Map<K, V>>>.contains(key: K, value: V) =
    isPresent().contains(key, value)
