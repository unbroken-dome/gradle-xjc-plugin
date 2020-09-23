package org.unbrokendome.gradle.plugins.xjc

import assertk.all
import assertk.assertThat
import assertk.assertions.isFile
import org.gradle.testkit.runner.GradleRunner
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.containsEntries
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.doesNotContainEntries
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.isSuccess
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.resolve
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.task
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.withJarFile
import org.unbrokendome.gradle.plugins.xjc.testutil.runGradle
import java.io.File


@UseSampleProject("episodes")
class EpisodesIntegrationTest {

    @TestEachDslFlavor
    @Testable
    fun test(runner: GradleRunner, @GradleProjectDir projectDir: File) {
        val buildResult = runner.runGradle("build")

        assertThat(buildResult).all {
            task(":episode-producer:xjcGenerate").isSuccess()
            task(":episode-producer:build").isSuccess()
            task(":episode-consumer:xjcGenerate").isSuccess()
            task(":episode-consumer:build").isSuccess()
        }

        // Should generate books schema classes and episode file in episode producer
        assertThat(projectDir, "projectDir")
            .resolve("episode-producer/build/libs/episode-producer.jar")
            .withJarFile {
                containsEntries(
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BookType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BooksType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/ObjectFactory.class",
                    "META-INF/sun-jaxb.episode"
                )
            }

        // Should generate bookshelf schema classes in episode consumer
        assertThat(projectDir, "projectDir")
            .resolve("episode-consumer/build/libs/episode-consumer.jar")
            .withJarFile {
                containsEntries(
                    "org/unbroken_dome/gradle_xjc_plugin/samples/bookshelf/Bookshelf.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/bookshelf/ObjectFactory.class"
                )
                doesNotContainEntries(
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BookType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BooksType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/ObjectFactory.class"
                )
            }
    }
}
