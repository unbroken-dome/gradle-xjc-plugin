package org.unbrokendome.gradle.plugins.xjc

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assumptions
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import org.unbrokendome.gradle.plugins.xjc.testutil.JavaVersionUtil.Companion.javaVersionAtLeast
import java.io.File


@UseSampleProject("xjc-tool-4_0")
class XjcTool40IntegrationTest : AbstractBasicIntegrationTest() {

    @TestEachDslFlavor
    @Testable
    fun test(runner: GradleRunner, @GradleProjectDir projectDir: File) {
        Assumptions.assumeTrue(javaVersionAtLeast(11), "Requires JDK11 runtime or above due to the XJC Tool bytecode version requirement")

        super.test(runner, projectDir, "xjc-tool-4_0")
    }
}
