package org.unbrokendome.gradle.plugins.xjc.testutil.assertions

import assertk.Assert


fun <T> Assert<*>.asType(): Assert<T> = transform { actual ->
    @Suppress("UNCHECKED_CAST")
    actual as T
}
