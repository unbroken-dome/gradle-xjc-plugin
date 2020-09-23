package org.unbrokendome.gradle.plugins.xjc.samples

import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.util.AnnotationUtils
import org.unbrokendome.gradle.plugins.xjc.testlib.directory
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleProjectExtension
import org.unbrokendome.gradle.plugins.xjc.testutil.GradleRunnerExtension
import java.util.stream.Stream
import kotlin.reflect.full.findAnnotation
import kotlin.streams.asStream


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@ExtendWith(GradleRunnerExtension::class, UseSampleProjectExtension::class)
annotation class UseSampleProject(val value: String)


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@TestTemplate
annotation class TestEachDslFlavor


private object NamespaceKey


private val namespace = ExtensionContext.Namespace.create(NamespaceKey)


private val ExtensionContext.store: ExtensionContext.Store
    get() = getStore(namespace)


private object SampleProjectNameKey


class UseSampleProjectExtension
@JvmOverloads constructor(
    private val sampleProjectName: String? = null
) : BeforeAllCallback, TestTemplateInvocationContextProvider {

    override fun beforeAll(context: ExtensionContext) {
        val useSampleProjectName = sampleProjectName ?:
            context.requiredTestClass.kotlin.findAnnotation<UseSampleProject>()?.value

        if (useSampleProjectName != null) {
            context.store.put(SampleProjectNameKey, useSampleProjectName)
        }
    }


    override fun supportsTestTemplate(context: ExtensionContext): Boolean =
        AnnotationUtils.isAnnotated(context.requiredTestMethod, TestEachDslFlavor::class.java)


    override fun provideTestTemplateInvocationContexts(
        context: ExtensionContext
    ): Stream<TestTemplateInvocationContext> {

        val sampleProjectName = context.store.get(SampleProjectNameKey, String::class.java)
            ?: this.sampleProjectName
            ?: throw IllegalStateException("Sample project name is not available in current context")

        return sequenceOf("kotlin-dsl", "groovy-dsl")
            .map { dslFlavor ->
                SampleProjectInvocationContext(sampleProjectName, dslFlavor)
            }
            .asStream()
    }


    private class SampleProjectInvocationContext(
        private val sampleProjectName: String,
        private val dslFlavor: String
    ) : TestTemplateInvocationContext {

        override fun getDisplayName(invocationIndex: Int): String =
            "[$dslFlavor]"


        override fun getAdditionalExtensions(): List<Extension> =
            listOf(
                GradleProjectExtension { projectDir ->
                    directory(projectDir) {
                        extractSamples("$dslFlavor/$sampleProjectName/")
                    }
                }
            )
    }
}
