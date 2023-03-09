package org.unbrokendome.gradle.plugins.xjc

import assertk.all
import assertk.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.containsEntries
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.containsEntry
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.isSuccess
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.resolve
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.task
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.withJarFile
import org.unbrokendome.gradle.plugins.xjc.testutil.runGradle
import java.io.File


@UseSampleProject("catalogs")
class CatalogsWithCleanIntegrationTest {

    @TestEachDslFlavor
    @Testable
    fun test(runner: GradleRunner, @GradleProjectDir projectDir: File) {
        val buildResult = runner.runGradle("build")

        assertThat(buildResult).all {
            task(":schema-library:build").isSuccess()
            task(":consumer:xjcGenerate").isSuccess()
            task(":consumer:build").isSuccess()
        }

        assertThat(projectDir, "projectDir")
            .resolve("schema-library/build/libs/schema-library.jar")
            .withJarFile {
                containsEntry("schema/books.xsd")
            }

        assertThat(projectDir, "projectDir")
            .resolve("consumer/build/libs/consumer.jar")
            .withJarFile {
                containsEntries(
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BookType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BooksType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/ObjectFactory.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/bookshelf/Bookshelf.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/bookshelf/ObjectFactory.class"
                )
            }

        // It has been observed that clean fails to delete the schema-library\build\libs\schema-library.jar
        //  after Gradle should have finished all processing with it.
        // Note this maybe a Windows platform issue only, as by default you can't
        //  unlink a file with an open file handle on it.
        var cleanResult = runner.runGradle("clean")

        assertThat(cleanResult).all {
            task(":schema-library:clean").isSuccess()
            task(":consumer:clean").isSuccess()
        }

    }
}
