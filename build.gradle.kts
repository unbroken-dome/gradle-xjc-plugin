plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("org.unbroken-dome.test-sets") version "4.0.0"
    id("com.gradle.plugin-publish") version "1.1.0"
    id("org.asciidoctor.jvm.convert") version "3.3.2"
    id("org.jetbrains.dokka") version "1.8.10"
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
val xjc30: SourceSet by sourceSets.creating
val xjc40: SourceSet by sourceSets.creating
val xjcSourceSets = listOf(xjcCommon, xjc22, xjc23, xjc30, xjc40)

val asciidoctorExtensions by configurations.creating

configurations {
    xjcSourceSets.forEach { xjcSourceSet ->
        (xjcSourceSet.compileOnlyConfigurationName) {
            extendsFrom(configurations["compileOnly"], configurations["implementation"])
        }
    }
}

dependencies {
    asciidoctorExtensions("com.bmuschko:asciidoctorj-tabbed-code-extension:0.3")

    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(gradleApi())

    implementation("javax.activation:javax.activation-api:1.2.0")
    implementation("xml-resolver:xml-resolver:1.2")

    xjcSourceSets.forEach { xjcSourceSet ->
        (xjcSourceSet.compileOnlyConfigurationName)(sourceSets["main"].output)
    }

    "xjcCommonCompileOnly"("com.sun.xml.bind:jaxb-xjc:2.3.3")

    "xjc22CompileOnly"(xjcCommon.output)
    "xjc22CompileOnly"("com.sun.xml.bind:jaxb-xjc:2.2.11")
    "xjc22CompileOnly"("com.sun.xml.bind:jaxb-core:2.2.11")
    "xjc22CompileOnly"("com.sun.xml.bind:jaxb-impl:2.2.11")
    "xjc22CompileOnly"("javax.xml.bind:jaxb-api:2.2.11")

    "xjc23CompileOnly"(xjcCommon.output)
    "xjc23CompileOnly"("com.sun.xml.bind:jaxb-xjc:2.3.3")

    "xjc30CompileOnly"(xjcCommon.output)
    "xjc30CompileOnly"("com.sun.xml.bind:jaxb-xjc:3.0.0")
    "xjc30CompileOnly"("com.sun.xml.bind:jaxb-xjc:2.3.3")
    "xjc30CompileOnly"("jakarta.xml.bind:jakarta.xml.bind-api:3.0.0")

    "xjc40CompileOnly"(xjcCommon.output)
    "xjc40CompileOnly"("com.sun.xml.bind:jaxb-xjc:4.0.2")
    "xjc40CompileOnly"("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")

    "testLibApi"(kotlin("stdlib-jdk8"))
    "testLibApi"("com.willowtreeapps.assertk:assertk-jvm:0.25")

    "testImplementation"("org.spekframework.spek2:spek-dsl-jvm:2.0.19")
    "testRuntimeOnly"("org.spekframework.spek2:spek-runner-junit5:2.0.19")

    "integrationTestImplementation"(gradleTestKit())
    "integrationTestImplementation"("org.junit.jupiter:junit-jupiter-api:5.9.2")
    "integrationTestImplementation"("org.junit.platform:junit-platform-commons:1.9.1")
    "integrationTestImplementation"("org.ow2.asm:asm:9.4")
    "integrationTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}


configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(kotlinVersion)
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
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


gradlePlugin {
    website.set("https://github.com/unbroken-dome/gradle-xjc-plugin")
    vcsUrl.set("https://github.com/unbroken-dome/gradle-xjc-plugin")

    (plugins) {
        "xjcPlugin" {
            displayName = "XJC Plugin"
            description = "A plugin that integrates the XJC binding compiler into a Gradle build."
            tags.set(listOf("xjc", "jaxb", "code generation", "xml"))
        }
    }
}


tasks.withType(org.jetbrains.dokka.gradle.DokkaTask::class).configureEach {
    dokkaSourceSets  {
        configureEach {
            externalDocumentationLink {
                url.set(uri("https://docs.gradle.org/current/javadoc/").toURL())
            }
            reportUndocumented.set(false)
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(uri("https://github.com/unbroken-dome/gradle-xjc-plugin/blob/v${project.version}/src/main/kotlin").toURL())
                remoteLineSuffix.set("#L")
            }
            perPackageOption {
                matchingRegex.set("""org\.unbrokendome\.gradle\.plugins\.xjc\.internal""")
                suppress.set(true)
            }
        }
    }
}

tasks.named("asciidoctor", org.asciidoctor.gradle.jvm.AsciidoctorTask::class) {
    setSourceDir(file("docs"))
    baseDirFollowsSourceFile()

    options(mapOf(
        "doctype" to "book"
    ))
    attributes(mapOf(
        "project-version" to project.version,
        "source-highlighter" to "prettify"
    ))
}
