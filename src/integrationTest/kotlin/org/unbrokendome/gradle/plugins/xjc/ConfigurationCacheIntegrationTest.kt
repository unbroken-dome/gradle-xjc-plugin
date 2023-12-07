package org.unbrokendome.gradle.plugins.xjc

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import org.gradle.testkit.runner.GradleRunner
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.TestEachDslFlavor
import org.unbrokendome.gradle.plugins.xjc.samples.UseSampleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectDir
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.output
import org.unbrokendome.gradle.plugins.xjc.testutil.runGradle
import java.io.File


@UseSampleProject("basic")
class ConfigurationCacheIntegrationTest : AbstractBasicIntegrationTest() {

    @TestEachDslFlavor
    @Testable
    fun test(runner: GradleRunner, @GradleProjectDir projectDir: File) {

        val projectName = "basic"
        val buildResult = runner.runGradle("--configuration-cache", "build")
        checkTaskSuccess(buildResult)
        checkOutputJar(projectDir, projectName)

        assertThat(buildResult).all {
            output().contains("Configuration cache entry stored.")
        }


        val maybeCachedBuildResult = runner.runGradle("--configuration-cache", "build")
        checkTaskUpToDate(maybeCachedBuildResult)

        assertThat(maybeCachedBuildResult).all {
            output().contains("Configuration cache entry reused.")
        }
    }
}
