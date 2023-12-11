import org.jetbrains.dokka.gradle.DokkaTask
import org.asciidoctor.gradle.AsciidoctorTask

plugins {
    kotlin("jvm")
    id("org.asciidoctor.convert") version "2.4.0"
    id("org.jetbrains.dokka") version "0.10.1"
}

val kotlinVersion: String by extra


repositories {
    mavenCentral()
}


fun resolveSystemGetenv(name: String, defaultValue: String? = null): String? {
    if(System.getenv().containsKey(name))
        return System.getenv(name)
    return defaultValue
}

val githubRepositoryOwner = resolveSystemGetenv("GITHUB_REPOSITORY_OWNER", "unbroken-dome")


tasks.named("dokka", org.jetbrains.dokka.gradle.DokkaTask::class) {
//tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    outputFormat = "html"
    configuration {
        externalDocumentationLink {
            url = uri("https://docs.gradle.org/current/javadoc/").toURL()
        }
        reportUndocumented = false
        sourceLink {
            path = "src/main/kotlin"
            url = "https://github.com/${githubRepositoryOwner}/gradle-xjc-plugin/blob/v${project.version}/src/main/kotlin"
            lineSuffix = "#L"
        }
        perPackageOption {
            prefix = "org.unbrokendome.gradle.plugins.xjc.internal"
            suppress = true
        }
    }
}


asciidoctorj {
    version = "2.4.1"
}

dependencies {
    "asciidoctor"("com.bmuschko:asciidoctorj-tabbed-code-extension:0.3")
}


tasks.named("asciidoctor", org.asciidoctor.gradle.AsciidoctorTask::class) {
//tasks.withType<org.asciidoctor.gradle.AsciidoctorTask>().configureEach {
    sourceDir(".")
    sources(delegateClosureOf<PatternSet> { include("index.adoc") })

    options(mapOf(
        "doctype" to "book"
    ))
    attributes(mapOf(
        "GITHUB_REPOSITORY_OWNER" to githubRepositoryOwner,
        "github-pages-uri" to "https://${githubRepositoryOwner}.github.io/gradle-xjc-plugin",
        "github-uri" to "https://github.com/${githubRepositoryOwner}/gradle-xjc-plugin",
        "project-version" to project.version,
        "source-highlighter" to "prettify"
    ))
}
