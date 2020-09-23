package org.unbrokendome.gradle.plugins.xjc.testutil

import org.junit.jupiter.api.extension.ExtendWith


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@ExtendWith(GradleProjectExtension::class, GradleRunnerExtension::class)
annotation class GradleIntegrationTest
