package org.unbrokendome.gradle.plugins.xjc.testutil

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver


private val namespace = ExtensionContext.Namespace.create(GradleRunnerExtension::class.java)


private val ExtensionContext.store: ExtensionContext.Store
    get() = getStore(namespace)


typealias GradleRunnerModifier = GradleRunner.() -> GradleRunner


private object GradleRunnerModifierStoreKey


@Suppress("UNCHECKED_CAST")
private val ExtensionContext.gradleRunnerModifier: GradleRunnerModifier?
    get() = store.get(GradleRunnerModifierStoreKey) as GradleRunnerModifier?


private operator fun GradleRunnerModifier?.plus(other: GradleRunnerModifier?): GradleRunnerModifier? =
    when {
        this == null -> other
        other == null -> this
        else -> {
            { this@plus().other() }
        }
    }


class GradleRunnerExtension : ParameterResolver {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type == GradleRunner::class.java


    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val projectDir = extensionContext.gradleProjectDir
        val modifier = extensionContext.gradleRunnerModifier

        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withDebug(true)
            .forwardOutput()
            .let { modifier?.invoke(it) ?: it }
    }
}


class GradleRunnerModifierExtension(
    private val modifier: GradleRunnerModifier
) : BeforeEachCallback {

    override fun beforeEach(context: ExtensionContext) {
        context.store.put(
            GradleRunnerModifierStoreKey,
            context.gradleRunnerModifier + modifier
        )
    }
}
