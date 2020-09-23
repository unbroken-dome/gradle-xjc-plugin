package org.unbrokendome.gradle.plugins.xjc.testutil.assertions

import assertk.Assert
import java.io.File


fun Assert<File>.resolve(relative: String) = transform(name = "$name/$relative") { actual ->
    actual.resolve(relative)
}
