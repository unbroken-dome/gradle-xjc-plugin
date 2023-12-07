package org.unbrokendome.gradle.plugins.xjc

import assertk.all
import assertk.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProjectExtension
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.containsEntries
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.isSuccess
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.isUpToDate
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.resolve
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.task
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.withJarFile
import org.unbrokendome.gradle.plugins.xjc.testutil.runGradle
import java.io.File


abstract class AbstractBasicIntegrationTest {

    fun test(runner: GradleRunner, projectDir: File, projectName: String, vararg args: String): BuildResult {

        val runGradleArgs = if (args.isNotEmpty()) args else arrayOf("build")

        val buildResult = runner.runGradle(*runGradleArgs)

        checkTaskSuccess(buildResult)

        checkOutputJar(projectDir, projectName)

        return buildResult
    }

    fun checkTaskSuccess(buildResult: BuildResult) {
        assertThat(buildResult).all {
            task(":xjcGenerate").isSuccess()
            task(":build").isSuccess()
        }
    }

    fun checkTaskUpToDate(buildResult: BuildResult) {
        assertThat(buildResult).all {
            task(":xjcGenerate").isUpToDate()
            task(":build").isUpToDate()
        }
    }


    fun checkOutputJar(projectDir: File, projectName: String) {
        assertThat(projectDir, "projectDir")
            .resolve("build/libs/$projectName.jar")
            .withJarFile {
                containsEntries(
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BookType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BooksType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/ObjectFactory.class"
                )
            }
    }

}
