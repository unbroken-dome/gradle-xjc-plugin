package org.unbrokendome.gradle.plugins.xjc

import assertk.all
import assertk.assertThat
import assertk.assertions.containsAll
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.reflection.MethodInfo
import org.unbrokendome.gradle.plugins.xjc.reflection.URLClassLoader
import org.unbrokendome.gradle.plugins.xjc.reflection.loadClassInfo
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.containsEntries
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.isSuccess
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.resolve
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.task
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.withJarFile
import org.unbrokendome.gradle.plugins.xjc.testutil.runGradle


@UseSampleProject("xjc-plugin")
class XjcPluginIntegrationTest {

    @TestEachDslFlavor
    @Testable
    fun test(runner: GradleRunner, @GradleProjectDir projectDir: File) {
        val buildResult = runner.runGradle("build")

        assertThat(buildResult).all {
            task(":xjcGenerate").isSuccess()
            task(":build").isSuccess()
        }

        assertThat(projectDir, "projectDir")
            .resolve("build/libs/xjc-plugin.jar")
            .withJarFile {
                containsEntries(
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BookType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/BooksType.class",
                    "org/unbroken_dome/gradle_xjc_plugin/samples/books/ObjectFactory.class"
                )
            }

        // Generated classes should contain the equals, hashCode and toString methods (from JAXB-basics plugin)
        val classInfo = URLClassLoader(projectDir.resolve("build/libs/xjc-plugin.jar"))
            .loadClassInfo("org.unbroken_dome.gradle_xjc_plugin.samples.books.BookType")
        assertThat(classInfo.methods, name = "BookType methods")
            .containsAll(
                MethodInfo(name = "equals", descriptor = "(Ljava/lang/Object;)Z"),
                MethodInfo(name = "hashCode", descriptor = "()I"),
                MethodInfo(name = "toString", descriptor = "()Ljava/lang/String;")
            )
    }
}
