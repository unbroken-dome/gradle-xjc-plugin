package org.unbrokendome.gradle.plugins.xjc.testutil.assertions

import assertk.Assert
import assertk.assertions.containsAll
import assertk.assertions.containsOnly
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.api.Task
import org.unbrokendome.gradle.plugins.xjc.testutil.isSkipped


val Assert<Task>.taskDependencies
    get() = transform { actual ->
        actual.taskDependencies.getDependencies(actual)
    }


fun Assert<Task>.hasTaskDependency(taskName: String) = given { actual ->
    val dependencies = actual.taskDependencies.getDependencies(actual)
    if (dependencies.none { it.name == taskName }) {
        this.expected("to have a dependency on task \"${taskName}\", but dependencies were: ${show(dependencies)}")
    }
}


fun Assert<Task>.hasOnlyTaskDependency(taskName: String) = given { actual ->
    val dependencies = actual.taskDependencies.getDependencies(actual)
    if (dependencies.size != 1 || dependencies.firstOrNull()?.name != taskName) {
        this.expected("to have a single dependency on task \"${taskName}\", but dependencies were: ${show(dependencies)}")
    }
}


fun Assert<Task>.hasTaskDependencies(vararg taskNames: String, exactly: Boolean = false) = given { actual ->
    val dependencies = actual.taskDependencies.getDependencies(actual)

    val dependencyTaskNames = dependencies.map { it.name }.toSet()

    val assert = assertThat(dependencyTaskNames, name = "task \"${actual.name}\" dependencies")

    if (exactly) {
        assert.containsOnly(*taskNames)
    } else {
        assert.containsAll(*taskNames)
    }
}


fun Assert<Task>.doesNotHaveTaskDependency(taskName: String) = given { actual ->
    val dependencies = actual.taskDependencies.getDependencies(actual)
    if (dependencies.any { it.name == taskName }) {
        this.expected("to have no dependency on task \"${taskName}\", but dependencies were: ${show(dependencies)}")
    }
}


fun Assert<Task>.isSkipped() = given { actual ->
    if (!actual.isSkipped()) {
        this.expected("to be skipped, but was not skipped")
    }
}


fun Assert<Task>.isNotSkipped() = given { actual ->
    if (actual.isSkipped()) {
        this.expected("not to be skipped, but was skipped")
    }
}
