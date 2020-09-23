package org.unbrokendome.gradle.plugins.xjc.testutil

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.platform.commons.util.AnnotationUtils
import org.junit.platform.commons.util.ReflectionUtils
import org.unbrokendome.gradle.plugins.xjc.testlib.DirectoryBuilder
import org.unbrokendome.gradle.plugins.xjc.testlib.directory
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import java.nio.file.Files
import java.nio.file.Path


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class GradleProjectSetup


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class GradleProjectDir


private object GradleProjectNamespaceKey


private val namespace = ExtensionContext.Namespace.create(GradleProjectNamespaceKey)


private val ExtensionContext.store: ExtensionContext.Store
    get() = getStore(namespace)


private class GradleProjectDirResource : ExtensionContext.Store.CloseableResource {

    companion object StoreKey

    val directory: File = Files.createTempDirectory("gradle").toFile()

    override fun close() {
        directory.deleteRecursively()
    }
}


typealias ProjectDirInitializer = (projectDir: File, context: ExtensionContext) -> Unit


private object ProjectDirInitializerStoreKey


private operator fun ProjectDirInitializer?.plus(other: ProjectDirInitializer?): ProjectDirInitializer? {
    return when {
        this == null -> other
        other == null -> this
        else -> {
            { projectDir, context ->
                this(projectDir, context)
                other(projectDir, context)
            }
        }
    }
}


private fun methodProjectDirInitializer(method: Method): ProjectDirInitializer = { projectDir, context ->

    val target = if (method.modifiers and Modifier.STATIC == 0) {
        context.requiredTestInstance
    } else null

    val args = method.parameters.map<Parameter, Any> { param ->
        when (param.type) {
            File::class.java -> projectDir
            Path::class.java -> projectDir.toPath()
            DirectoryBuilder::class.java -> DirectoryBuilder(projectDir)
            else -> error("Cannot resolve argument for parameter \"${param.name}\" @SetupGradleProject method $method")
        }
    }

    method.invoke(target, *args.toTypedArray())
}


@Suppress("UNCHECKED_CAST")
private val ExtensionContext.projectDirInitializer: ProjectDirInitializer?
    get() = store.get(ProjectDirInitializerStoreKey) as ProjectDirInitializer?


fun ExtensionContext.setupProjectDir(initializer: (projectDir: File) -> Unit) {
    store.put(
        ProjectDirInitializerStoreKey,
        this.projectDirInitializer + { projectDir, _ -> initializer(projectDir) }
    )
}


val ExtensionContext.gradleProjectDir: File
    get() {
        val resource = store.getOrComputeIfAbsent(GradleProjectDirResource, {
            GradleProjectDirResource().also {
                initGradleProject(it.directory)
            }
        }, GradleProjectDirResource::class.java)
        return resource.directory
    }


private fun ExtensionContext.initGradleProject(projectDir: File) {
    projectDirInitializer?.invoke(projectDir, this)

    // Create a settings file if the test setup didn't create one, otherwise Gradle searches up the
    // directory hierarchy (and might actually find one)
    directory(projectDir) {
        if (!Files.exists(path.resolve("settings.gradle")) &&
            !Files.exists(path.resolve("settings.gradle.kts"))
        ) {
            file(
                name = "settings.gradle",
                contents = """
                    rootProject.name = '${projectDir.name}'
                    """.trimIndent()
            )
        }
    }
}


class GradleProjectExtension
@JvmOverloads constructor(
    private val projectDirInitializer: ((projectDir: File) -> Unit)? = null
) : BeforeAllCallback, BeforeEachCallback, ParameterResolver {

    @ExperimentalStdlibApi
    override fun beforeAll(context: ExtensionContext) {
        val initializerFromMethods = AnnotationUtils.findAnnotatedMethods(
            context.requiredTestClass, GradleProjectSetup::class.java,
            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN
        ).asSequence()
            .map(::methodProjectDirInitializer)
            .reduceOrNull { acc, initializer -> (acc + initializer)!! }

        if (initializerFromMethods != null) {
            context.store.put(
                ProjectDirInitializerStoreKey,
                context.projectDirInitializer + initializerFromMethods
            )
        }
    }


    @Suppress("UNCHECKED_CAST")
    override fun beforeEach(context: ExtensionContext) {
        if (projectDirInitializer != null) {
            context.setupProjectDir(projectDirInitializer)
        }
    }


    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean =
        parameterContext.isAnnotated(GradleProjectDir::class.java) &&
                parameterContext.parameter.type in setOf(File::class.java, Path::class.java)


    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
        val projectDir = extensionContext.gradleProjectDir
        return when (parameterContext.parameter.type) {
            File::class.java -> projectDir
            Path::class.java -> projectDir.toPath()
            else -> error("Invalid parameter type")
        }
    }
}
