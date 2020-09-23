package org.unbrokendome.gradle.plugins.xjc.testutil

import org.gradle.api.logging.Logging
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.util.AnnotationUtils
import org.slf4j.LoggerFactory
import java.util.stream.Stream
import kotlin.streams.asStream


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@ExtendWith(GradleVersionsExtension::class)
annotation class GradleVersions(vararg val value: String)


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@TestTemplate
annotation class TestEachGradleVersion


class GradleVersionsExtension : TestTemplateInvocationContextProvider {

    private val logger = Logging.getLogger(javaClass)


    override fun supportsTestTemplate(context: ExtensionContext): Boolean =
        AnnotationUtils.isAnnotated(context.requiredTestMethod, TestEachGradleVersion::class.java)


    override fun provideTestTemplateInvocationContexts(
        context: ExtensionContext
    ): Stream<TestTemplateInvocationContext> {

        val testClasses = generateSequence(context.requiredTestClass) { it.enclosingClass }

        val versions = testClasses
            .mapNotNull { testClass ->
                testClass.getAnnotation(GradleVersions::class.java)
            }
            .firstOrNull()
            ?.value?.toList()

        if (versions == null) {
            logger.warn(
                "Test method {} is annotated with @TestEachGradleVersion, but no Gradle versions are specified.",
                context.requiredTestMethod
            )
            return Stream.empty()
        }

        return versions.asSequence()
            .flatMap { gradleVersions ->
                gradleVersions.splitToSequence(',').map { it.trim() }
            }
            .map { gradleVersion ->
                GradleVersionTestTemplateInvocationContext(gradleVersion)
            }
            .asStream()
    }


    private class GradleVersionTestTemplateInvocationContext(
        private val gradleVersion: String
    ) : TestTemplateInvocationContext {

        private val logger = Logging.getLogger(javaClass)


        override fun getDisplayName(invocationIndex: Int): String =
            "[Gradle $gradleVersion]"


        override fun getAdditionalExtensions(): List<Extension> =
            listOf(
                GradleRunnerModifierExtension {
                    logger.lifecycle("Running with Gradle version {}", gradleVersion)
                    withGradleVersion(gradleVersion)
                        .withDebug(false)
                }
            )
    }
}
