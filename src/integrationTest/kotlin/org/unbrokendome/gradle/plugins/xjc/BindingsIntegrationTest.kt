package org.unbrokendome.gradle.plugins.xjc

import assertk.all
import assertk.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.*
import org.unbrokendome.gradle.plugins.xjc.testutil.runGradle
import java.io.File


@UseSampleProject("bindings")
class BindingsIntegrationTest {

    @TestEachDslFlavor
    @Testable
    fun test(runner: GradleRunner, @GradleProjectDir projectDir: File) {
        val projectName = "bindings"

        val buildResult = runner.runGradle("build")

        assertThat(buildResult).all {
            task(":xjcGenerate").isSuccess()
            task(":build").isSuccess()
        }

        assertThat(projectDir, "projectDir")
            .resolve("build/libs/$projectName.jar")
            .withJarFile {
                containsEntries(
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/alt/package_name/BookType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/alt/package_name/BooksType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/alt/package_name/ObjectFactory.class"
                )
            }

    }
}
