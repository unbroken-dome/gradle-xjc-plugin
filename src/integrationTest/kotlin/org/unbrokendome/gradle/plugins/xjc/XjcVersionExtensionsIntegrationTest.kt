package org.unbrokendome.gradle.plugins.xjc

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import org.gradle.testkit.runner.GradleRunner
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.*
import org.unbrokendome.gradle.plugins.xjc.testutil.runGradle
import java.io.File


@UseSampleProject("xjc-version-extensions")
class XjcVersionExtensionsIntegrationTest {

    @TestEachDslFlavor
    @Testable
    fun test(runner: GradleRunner, @GradleProjectDir projectDir: File) {
        val projectName = "xjc-version-extensions"

        val buildResult = runner.runGradle("build")

        assertThat(buildResult).all {
            task(":xjcGenerate").isSuccess()
            task(":build").isSuccess()
        }

        assertThat(projectDir, "projectDir")
            .resolve("build/libs/$projectName.jar")
            .withJarFile {
                containsEntries(
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/code_injector/BookType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/code_injector/BooksType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/code_injector/ObjectFactory.class"
                )
            }

        assertThat(projectDir, "projectDir")
            .resolve("build/generated/sources/xjc/java/main/org/unbroken_dome/gradle_xjc_plugin/samples/books/code_injector/ObjectFactory.java")
            .withReadTextFile {
                contains("javax.xml.bind.annotation.XmlRegistry")
                doesNotContain("jakarta")
                contains("javax.annotation.Generated")
                contains("@Generated(")
            }

        assertThat(projectDir, "projectDir")
            .resolve("build/generated/sources/xjc/java/main/org/unbroken_dome/gradle_xjc_plugin/samples/books/code_injector/BookType.java")
            .withReadTextFile {
                contains("javax.xml.bind.annotation.XmlType")
                doesNotContain("jakarta")
                contains("javax.annotation.Generated")
                contains("@Generated(")
                contains("com.sun.xml.bind.annotation.XmlLocation")
                contains(" synchronized ")
                contains(" injectedSystemCurrentTime")
            }
    }
}
