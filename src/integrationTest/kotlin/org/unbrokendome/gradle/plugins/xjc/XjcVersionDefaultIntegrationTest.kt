package org.unbrokendome.gradle.plugins.xjc

import org.gradle.testkit.runner.GradleRunner
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import java.io.File


@UseSampleProject("xjc-version-default")
class XjcVersionDefaultIntegrationTest : AbstractBasicIntegrationTest() {

    @TestEachDslFlavor
    @Testable
    fun test(runner: GradleRunner, @GradleProjectDir projectDir: File) {
        super.test(runner, projectDir, "xjc-version-default")
    }
}
