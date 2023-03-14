package org.unbrokendome.gradle.plugins.xjc

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import org.gradle.testkit.runner.GradleRunner
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.resolve
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.withReadTextFile
import java.io.File


@UseSampleProject("xjc-tool-3_0-target-2_3")
class XjcTool30Target23IntegrationTest : AbstractBasicIntegrationTest() {

    @TestEachDslFlavor
    @Testable
    fun test(runner: GradleRunner, @GradleProjectDir projectDir: File) {
        val projectName = "xjc-tool-3_0-target-2_3"

        super.test(runner, projectDir, projectName)

        assertThat(projectDir, "projectDir")
            .resolve("build/generated/sources/xjc/java/main/org/unbroken_dome/gradle_xjc_plugin/samples/books/ObjectFactory.java")
            .withReadTextFile {
                contains("javax.xml.bind.annotation.XmlRegistry")
                doesNotContain("jakarta")
            }
    }
}
