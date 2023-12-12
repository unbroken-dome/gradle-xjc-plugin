package org.unbrokendome.gradle.plugins.xjc.testutil.assertions

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.api.NamedDomainObjectCollection


fun <T : Any> Assert<NamedDomainObjectCollection<T>>.containsItem(name: String) =
    transform(name = "${this.name}[\"$name\"]") { actual ->
        actual.findByName(name) ?: this.expected("to contain an item named \"$name\"", actual = actual.names)
    }


fun <T : Any> Assert<NamedDomainObjectCollection<T>>.doesNotContainItem(name: String) = given { actual ->
    val item = actual.findByName(name)
    if (item != null) {
        this.expected("to contain no item named \"$name\", but did contain: ${show(item)}", actual = actual.names)
    }
}
