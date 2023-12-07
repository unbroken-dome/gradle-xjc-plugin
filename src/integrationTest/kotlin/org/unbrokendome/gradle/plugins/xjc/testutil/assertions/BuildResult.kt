package org.unbrokendome.gradle.plugins.xjc.testutil.assertions

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.isIn
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome


fun Assert<BuildResult>.task(taskPath: String) =
    transform(name = "task $taskPath") { actual ->
        actual.task(taskPath)
            ?: expected("to include task '$taskPath', but was: ${show(actual.tasks.map { it.path })}")
    }


fun Assert<BuildResult>.output() =
    transform(name = "output") { actual ->
        actual.output
    }


val Assert<BuildTask>.outcome: Assert<TaskOutcome>
    get() = transform(name = "$name outcome") { actual ->
        actual.outcome
    }


fun Assert<BuildTask>.hasOutcome(expected: TaskOutcome) =
    outcome.isEqualTo(expected)


fun Assert<BuildTask>.hasOutcome(vararg expected: TaskOutcome) =
    outcome.isIn(*expected)


fun Assert<BuildTask>.isSuccess() =
    hasOutcome(TaskOutcome.SUCCESS)


fun Assert<BuildTask>.isUpToDate() =
    hasOutcome(TaskOutcome.UP_TO_DATE)


fun Assert<BuildTask>.isSkipped() =
    hasOutcome(TaskOutcome.SKIPPED)
