package org.unbrokendome.gradle.plugins.xjc

import assertk.assertThat
import assertk.assertions.contains
import org.gradle.testkit.runner.GradleRunner
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.resolve
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.withReadTextFile
import java.io.File


@UseSampleProject("xjc-version-ext-basics-annox")
class XjcVersionExtBasicsAnnoxIntegrationTest : AbstractBasicIntegrationTest() {

    @TestEachDslFlavor
    @Testable
    fun test(runner: GradleRunner, @GradleProjectDir projectDir: File) {
        val projectName = "xjc-version-ext-basics-annox"

        super.test(runner, projectDir, projectName)

        assertThat(projectDir, "projectDir")
            .resolve("build/generated/sources/xjc/java/main/org/unbroken_dome/gradle_xjc_plugin/samples/books/BookType.java")
            .withReadTextFile {
                contains("BookTypeHasBeenRenamed")
            }

        assertThat(projectDir, "projectDir")
            .resolve("build/generated/sources/xjc/java/main/org/unbroken_dome/gradle_xjc_plugin/samples/books/BookType.java")
            .withReadTextFile {
                contains("authorFieldHasBeenRenamed")
            }
    }

}
