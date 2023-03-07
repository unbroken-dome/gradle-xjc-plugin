package org.unbrokendome.gradle.plugins.xjc

import assertk.all
import assertk.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Nested
import org.junit.platform.commons.annotation.Testable
import org.unbrokendome.gradle.plugins.xjc.samples.extractSamples
import org.unbrokendome.gradle.plugins.xjc.testlib.directory
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleIntegrationTest
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectSetup
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleVersions
import org.unbrokendome.gradle.plugins.xjc.testutil.TestEachGradleVersion
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.isSuccess
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.task
import org.unbrokendome.gradle.plugins.xjc.testutil.runGradle
import java.io.File


const val AllGradleVersions =  "8.0.2, 7.6.1, 7.5.1, 7.4.2, 7.3.3, 7.2, 7.1.1, 7.0.2, 6.9.4, 6.8.2, 6.7.1, 6.6.1, 6.5.1, 6.4.1, 6.3, 6.2.2, 6.1.1, 6.0.1, 5.6.4, 5.6"


@GradleIntegrationTest
@GradleVersions
class GradleVersionCompatibilityIntegrationTest {

    abstract class AbstractTest(private val samplePath: String) {

        @GradleProjectSetup
        fun setupGradleProject(projectDir: File) {
            directory(projectDir) {
                extractSamples(samplePath)
            }
        }


        @TestEachGradleVersion
        @Testable
        fun test(runner: GradleRunner) {
            val buildResult = runner.runGradle("build")

            assertThat(buildResult).all {
                task(":library:build").isSuccess()
                task(":consumer:build").isSuccess()
            }
        }
    }

    @Nested
    @GradleVersions(AllGradleVersions)
    inner class GroovyDsl : AbstractTest("groovy-dsl/complete/")

    @Nested
    @GradleVersions(AllGradleVersions)
    inner class KotlinDsl : AbstractTest("kotlin-dsl/complete/")
}
