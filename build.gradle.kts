plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("org.unbroken-dome.test-sets") version "3.0.1"
    id("com.gradle.plugin-publish") version "0.12.0"
    id("org.asciidoctor.convert") version "1.5.9.2"
    id("org.jetbrains.dokka") version "0.10.1"
}


val kotlinVersion: String by extra


repositories {
    mavenCentral()
}


testSets {
    val testLib by libraries.creating

    ("unitTest") {
        imports(testLib)
    }

    create("integrationTest") {
        imports(testLib)
    }
}


val xjcCommon: SourceSet by sourceSets.creating
val xjc22: SourceSet by sourceSets.creating
val xjc23: SourceSet by sourceSets.creating
val xjc24: SourceSet by sourceSets.creating
val xjc30: SourceSet by sourceSets.creating
val xjc40: SourceSet by sourceSets.creating
val xjcSourceSets = listOf(xjcCommon, xjc22, xjc23, xjc24, xjc30, xjc40)


dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(gradleApi())

    implementation("javax.activation:javax.activation-api:1.2.0")
    implementation("xml-resolver:xml-resolver:1.2")

    for (xjcSourceSet in xjcSourceSets) {
        (xjcSourceSet.compileOnlyConfigurationName)(sourceSets["main"].output)
        (xjcSourceSet.compileOnlyConfigurationName)(configurations["compileOnly"])
        (xjcSourceSet.implementationConfigurationName)(configurations["implementation"])
    }

    "xjcCommonCompileOnly"("com.sun.xml.bind:jaxb-xjc:2.3.3")

    "xjc22CompileOnly"(xjcCommon.output)
    "xjc22CompileOnly"("com.sun.xml.bind:jaxb-xjc:2.2.11")
    "xjc22CompileOnly"("com.sun.xml.bind:jaxb-core:2.2.11")
    "xjc22CompileOnly"("com.sun.xml.bind:jaxb-impl:2.2.11")
    "xjc22CompileOnly"("javax.xml.bind:jaxb-api:2.2.11")

    "xjc23CompileOnly"(xjcCommon.output)
    "xjc23CompileOnly"("com.sun.xml.bind:jaxb-xjc:2.3.8")
    "xjc23CompileOnly"("com.sun.xml.bind:jaxb-core:2.3.0.1")
    "xjc23CompileOnly"("com.sun.xml.bind:jaxb-impl:2.3.8")
    "xjc23CompileOnly"("javax.xml.bind:jaxb-api:2.3.1")

    "xjc24CompileOnly"(xjcCommon.output)
    "xjc24CompileOnly"("com.sun.xml.bind:jaxb-xjc:2.4.0-b180830.0438")
    "xjc24CompileOnly"("com.sun.xml.bind:jaxb-impl:2.4.0-b180830.0438")
    "xjc22CompileOnly"("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")

    "xjc30CompileOnly"(xjcCommon.output)
    "xjc30CompileOnly"("com.sun.xml.bind:jaxb-xjc:3.0.2")
    "xjc30CompileOnly"("com.sun.xml.bind:jaxb-core:3.0.2")
    "xjc30CompileOnly"("com.sun.xml.bind:jaxb-impl:3.0.2")
    "xjc30CompileOnly"("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")

    "xjc40CompileOnly"(xjcCommon.output)
    "xjc40CompileOnly"("com.sun.xml.bind:jaxb-xjc:4.0.2")
    "xjc40CompileOnly"("com.sun.xml.bind:jaxb-core:4.0.2")
    "xjc40CompileOnly"("com.sun.xml.bind:jaxb-impl:4.0.2")
    "xjc40CompileOnly"("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")

    "testLibApi"(kotlin("stdlib-jdk8"))
    "testLibApi"("com.willowtreeapps.assertk:assertk-jvm:0.22")

    "testImplementation"("org.spekframework.spek2:spek-dsl-jvm:2.0.9")
    "testRuntimeOnly"("org.spekframework.spek2:spek-runner-junit5:2.0.9")

    "integrationTestImplementation"(gradleTestKit())
    "integrationTestImplementation"("org.junit.jupiter:junit-jupiter-api:5.7.0")
    "integrationTestImplementation"("org.junit.platform:junit-platform-commons:1.7.0")
    "integrationTestImplementation"("org.ow2.asm:asm:9.0")
    "integrationTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}


configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(kotlinVersion)
        }
    }
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
}


tasks.named<Jar>("jar") {
    for (xjcSourceSet in xjcSourceSets) {
        from(xjcSourceSet.output)
    }
}


tasks.withType<Test> {
    // always execute tests
    outputs.upToDateWhen { false }

    useJUnitPlatform()

    testLogging.showStandardStreams = true

    // give tests a temporary directory below the build dir so
    // we don't pollute the system temp dir (Gradle tests don't clean up)
    val tmpDir = layout.buildDirectory.dir("tmp")
    systemProperty("java.io.tmpdir", tmpDir.get())

    doFirst {
        project.mkdir(tmpDir)
    }
}


val samples = fileTree("samples") { exclude("**/build/**", "**/.gradle/**") }

val createSamplesIndex: Task by tasks.creating {
    val outputDir = layout.buildDirectory.file("generated/samples-index")
    inputs.dir("samples")
    outputs.dir(outputDir)
    doLast {
        project.mkdir(outputDir)
        val outputFile = file(outputDir).resolve("samples-index.txt")
        outputFile.bufferedWriter().use { writer ->
            samples.visit {
                if (!this.isDirectory) {
                    writer.appendln(this.path)
                }
            }
        }
    }
}

sourceSets.named("integrationTest") {
    resources {
        srcDir("samples")
        exclude("**/build/**", "**/.gradle/**")
        srcDir(createSamplesIndex)
    }
}


val pluginUnderTestMetadata by tasks.existing(PluginUnderTestMetadata::class) {
    for (xjcSourceSet in xjcSourceSets) {
        pluginClasspath.from(xjcSourceSet.output)
    }
}

tasks.named("processIntegrationTestResources") {
    dependsOn(pluginUnderTestMetadata)
}


gradlePlugin {
    isAutomatedPublishing = true

    testSourceSets(project.sourceSets["integrationTest"])

    plugins {
        create("xjcPlugin") {
            id = "org.unbroken-dome.xjc"
            implementationClass = "org.unbrokendome.gradle.plugins.xjc.XjcPlugin"
        }
    }
}


pluginBundle {
    website = "https://github.com/unbroken-dome/gradle-xjc-plugin"
    vcsUrl = "https://github.com/unbroken-dome/gradle-xjc-plugin"
    description = "A plugin that integrates the XJC binding compiler into a Gradle build."
    tags = listOf("xjc", "jaxb", "code generation", "xml")

    (plugins) {
        "xjcPlugin" {
            displayName = "XJC Plugin"
        }
    }
}


tasks.named("dokka", org.jetbrains.dokka.gradle.DokkaTask::class) {
    outputFormat = "html"
    configuration {
        externalDocumentationLink {
            url = uri("https://docs.gradle.org/current/javadoc/").toURL()
        }
        reportUndocumented = false
        sourceLink {
            path = "src/main/kotlin"
            url = "https://github.com/unbroken-dome/gradle-xjc-plugin/blob/v${project.version}/src/main/kotlin"
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
    sourceDir("docs")
    sources(delegateClosureOf<PatternSet> { include("index.adoc") })

    options(mapOf(
        "doctype" to "book"
    ))
    attributes(mapOf(
        "project-version" to project.version,
        "source-highlighter" to "prettify"
    ))
}

apply {
     from("${rootDir}/publish.gradle.kts")
}
