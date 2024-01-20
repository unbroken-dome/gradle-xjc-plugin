package org.unbrokendome.gradle.plugins.xjc.testutil.assertions

import assertk.Assert
import assertk.all
import assertk.assertions.isFile
import java.io.File


fun Assert<File>.withReadTextFile(block: Assert<String>.() -> Unit) {
    isFile()
    given { actual ->
        val text = actual.readText(Charsets.UTF_8)
        assertThat(text, "readText:$name").all(block)
    }
}
