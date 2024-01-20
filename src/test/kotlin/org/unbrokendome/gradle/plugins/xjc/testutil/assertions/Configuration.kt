package org.unbrokendome.gradle.plugins.xjc.testutil.assertions

import assertk.Assert
import assertk.assertions.contains
import assertk.assertions.prop
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.api.artifacts.Configuration


fun Assert<Configuration>.extendsFrom(other: String) = given { actual ->
    if (other in actual.extendsFrom.map { it.name }) return
    this.expected(
        "to extend from configuration \"$other\", but extends from: ${show(actual.extendsFrom)}",
        actual = actual.extendsFrom, expected = other
    )
}


fun Assert<Configuration>.extendsOnlyFrom(vararg others: String) = given { actual ->
    val extendsFromNames = actual.extendsFrom.map { it.name }.toSet()
    if (others.toSet() == extendsFromNames) return

    this.expected(
        "to extend only from configuration(s) ${show(others)}, but extends from: ${show(extendsFromNames)}",
        actual = extendsFromNames, expected = others
    )
}
