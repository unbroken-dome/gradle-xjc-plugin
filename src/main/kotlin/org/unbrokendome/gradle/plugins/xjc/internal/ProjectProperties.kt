package org.unbrokendome.gradle.plugins.xjc.internal

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.Serializable


internal fun Project.providerFromProjectProperty(
    propertyName: String, defaultValue: String? = null
): Provider<String> =
    provider {
        findProperty(propertyName)?.toString() ?: defaultValue
    }


internal fun Project.booleanProviderFromProjectProperty(
    propertyName: String, defaultValue: Boolean? = null
): Provider<Boolean> =
    providerFromProjectProperty(
        propertyName, transform = { it.toBoolean() }, defaultValue = defaultValue
    )


internal fun <T : Serializable> Project.providerFromProjectProperty(
    propertyName: String, transform: (String) -> T, defaultValue: T? = null
): Provider<T> = provider {
    findProperty(propertyName)?.toString()?.let(transform) ?: defaultValue
}
