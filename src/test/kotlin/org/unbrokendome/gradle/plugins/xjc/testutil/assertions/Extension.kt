package org.unbrokendome.gradle.plugins.xjc.testutil.assertions

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.ExtensionAware
import kotlin.reflect.KClass


fun Assert<Any>.hasExtensionNamed(name: String): Assert<Any> =
    transform("extension \"$name\"") { actual ->
        if (actual !is ExtensionAware) {
            this.expected("to be ExtensionAware")
        }
        actual.extensions.findByName(name)
            ?: this.expected("to have an extension named \"$name\"")
    }


inline fun <reified E : Any> Assert<Any>.hasExtension(name: String? = null): Assert<E> =
    transform("extension " + (name?.let { "\"$it\"" } ?: show(E::class))) { actual ->
        if (actual !is ExtensionAware) {
            this.expected("to be ExtensionAware")
        }
        val extensions = actual.extensions

        if (name != null) {
            val extension = extensions.findByName(name)
                ?: this.expected("to have an extension named \"$name\" of type ${show(E::class)}")
            (extension as? E)
                ?: this.expected(
                    "to have an extension named \"$name\" of type ${show(E::class)}, " +
                            "but actual type was: ${show(extension.javaClass)}"
                )

        } else {
            extensions.findByType(E::class.java)
                ?: this.expected("to have an extension of type ${show(E::class)}")
        }
    }


fun <C : Any> Assert<Any>.hasConvention(type: KClass<C>): Assert<C> =
    transform("convention ${show(type)}") { actual ->
        if (actual !is HasConvention) {
            this.expected("to support conventions")
        }
        actual.convention.findPlugin(type.java)
            ?: this.expected("to have a convention plugin of type ${show(type)}")
    }


inline fun <reified C : Any> Assert<Any>.hasConvention(): Assert<C> =
    this.hasConvention(C::class)
