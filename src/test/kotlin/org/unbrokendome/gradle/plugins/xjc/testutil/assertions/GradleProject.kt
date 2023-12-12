package org.unbrokendome.gradle.plugins.xjc.testutil.assertions

import assertk.Assert
import assertk.assertions.isInstanceOf
import assertk.assertions.support.expected
import org.gradle.api.Project
import org.gradle.api.Task
import kotlin.reflect.KClass


private fun Assert<Project>.containsTaskInternal(taskName: String) =
    transform("task \"$taskName\"") { actual ->
        actual.tasks.findByName(taskName)
            ?: this.expected("to contain a task named \"$taskName\"")
    }


fun <T : Task> Assert<Project>.containsTask(taskName: String, taskType: KClass<T>) =
    containsTaskInternal(taskName)
        .isInstanceOf(taskType)


inline fun <reified T : Task> Assert<Project>.containsTask(taskName: String) =
    this.containsTask(taskName, T::class)
