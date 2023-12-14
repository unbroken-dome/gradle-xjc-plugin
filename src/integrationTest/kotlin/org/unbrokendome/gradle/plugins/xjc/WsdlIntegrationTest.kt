package org.unbrokendome.gradle.plugins.xjc

import assertk.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.containsEntries
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.resolve
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.withJarFile
import java.io.File


@UseSampleProject("wsdl")
class WsdlIntegrationTest : AbstractBasicIntegrationTest() {

    @TestEachDslFlavor
    @Testable
    fun test(runner: GradleRunner, @GradleProjectDir projectDir: File) {
        val projectName = "wsdl"

        super.test(runner, projectDir, projectName)

        assertThat(projectDir, "projectDir")
            .resolve("build/libs/$projectName.jar")
            .withJarFile {
                containsEntries(
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BookType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BooksType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/ObjectFactory.class",

                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/alt/common/BookId.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/alt/common/ObjectFactory.class",

                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/alt/lookup/ObjectFactory.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/alt/lookup/SearchRequest.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/alt/lookup/SearchResult.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/alt/lookup/SearchResultItem.class"
                )
            }
    }
}
