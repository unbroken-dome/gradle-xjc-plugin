plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("org.darrylmiles.repack.org.unbroken-dome.test-sets") // version "$testSetsVersion"
    id("com.gradle.plugin-publish") version "0.21.0"
}


val kotlinVersion: String by extra
val testSetsVersion: String by extra


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
val xjc21: SourceSet by sourceSets.creating
val xjc22: SourceSet by sourceSets.creating
val xjc23: SourceSet by sourceSets.creating
val xjc24: SourceSet by sourceSets.creating
val xjc30: SourceSet by sourceSets.creating
val xjc40: SourceSet by sourceSets.creating
val xjcSourceSets = listOf(xjcCommon, xjc21, xjc22, xjc23, xjc24, xjc30, xjc40)


dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(gradleApi())

    implementation("javax.activation:javax.activation-api:1.2.0")
    implementation("xml-resolver:xml-resolver:1.2")

    for (xjcSourceSet in xjcSourceSets) {
        (xjcSourceSet.compileOnlyConfigurationName)(sourceSets["main"].output)

        // Gradle 8.x does not allow referencing configurations[] like this anymore
        // but it seems a convenience to unroll the deplist

        // for compileOnly()
        //(xjcSourceSet.compileOnlyConfigurationName)(configurations["compileOnly"].allDependencies)
        (xjcSourceSet.compileOnlyConfigurationName)(kotlin("stdlib-jdk8"))
        (xjcSourceSet.compileOnlyConfigurationName)(gradleApi())

        // for implementation()
        //(xjcSourceSet.implementationConfigurationName)(configurations["implementation"].allDependencies)
        (xjcSourceSet.implementationConfigurationName)("javax.activation:javax.activation-api:1.2.0")
        (xjcSourceSet.implementationConfigurationName)("xml-resolver:xml-resolver:1.2")
    }

    "xjcCommonCompileOnly"("com.sun.xml.bind:jaxb-xjc:2.3.3")

    "xjc21CompileOnly"(xjcCommon.output)
    "xjc21CompileOnly"("com.sun.xml.bind:jaxb-xjc:2.1.17")
    "xjc21CompileOnly"("com.sun.xml.bind:jaxb-core:2.1.14")
    "xjc21CompileOnly"("com.sun.xml.bind:jaxb-impl:2.1.17")
    "xjc21CompileOnly"("javax.xml.bind:jaxb-api:2.1")

    "xjc22CompileOnly"(xjcCommon.output)
    "xjc22CompileOnly"("com.sun.xml.bind:jaxb-xjc:2.2.11")
    "xjc22CompileOnly"("com.sun.xml.bind:jaxb-core:2.2.11")
    "xjc22CompileOnly"("com.sun.xml.bind:jaxb-impl:2.2.11")
    "xjc22CompileOnly"("javax.xml.bind:jaxb-api:2.2.11")

    "xjc23CompileOnly"(xjcCommon.output)
    "xjc23CompileOnly"("com.sun.xml.bind:jaxb-xjc:2.3.9")
    "xjc23CompileOnly"("com.sun.xml.bind:jaxb-core:2.3.0.1")
    "xjc23CompileOnly"("com.sun.xml.bind:jaxb-impl:2.3.9")
    "xjc23CompileOnly"("javax.xml.bind:jaxb-api:2.3.1")

    "xjc24CompileOnly"(xjcCommon.output)
    "xjc24CompileOnly"("com.sun.xml.bind:jaxb-xjc:2.4.0-b180830.0438")
    "xjc24CompileOnly"("com.sun.xml.bind:jaxb-impl:2.4.0-b180830.0438")
    "xjc24CompileOnly"("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")

    "xjc30CompileOnly"(xjcCommon.output)
    "xjc30CompileOnly"("com.sun.xml.bind:jaxb-xjc:3.0.2")
    "xjc30CompileOnly"("com.sun.xml.bind:jaxb-core:3.0.2")
    "xjc30CompileOnly"("com.sun.xml.bind:jaxb-impl:3.0.2")
    "xjc30CompileOnly"("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")

    "xjc40CompileOnly"(xjcCommon.output)
    "xjc40CompileOnly"("com.sun.xml.bind:jaxb-xjc:4.0.4")
    "xjc40CompileOnly"("com.sun.xml.bind:jaxb-core:4.0.4")
    "xjc40CompileOnly"("com.sun.xml.bind:jaxb-impl:4.0.4")
    "xjc40CompileOnly"("jakarta.xml.bind:jakarta.xml.bind-api:4.0.1")

    "testLibApi"(kotlin("stdlib-jdk8"))
    // Bumping past 0.22 forces kotlin 1.4.x (Gradle 7+)
    "testLibApi"("com.willowtreeapps.assertk:assertk-jvm:0.22")

    // Bumping these past 2.0.15 forces Gradle 7.x use for newer kotlin
    "testImplementation"("org.spekframework.spek2:spek-dsl-jvm:2.0.15")
    // 2.0.16 requires Java11 runtime, 2.0.17 reverted back to Java8
    // But needs Gradle 7.x kotlin
    "testRuntimeOnly"("org.spekframework.spek2:spek-runner-junit5:2.0.15")

    "integrationTestImplementation"(gradleTestKit())
    "integrationTestImplementation"("org.junit.jupiter:junit-jupiter-api:5.7.0")
    "integrationTestImplementation"("org.junit.platform:junit-platform-commons:1.7.0")
    "integrationTestImplementation"("org.ow2.asm:asm:9.5")
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
    if(kotlinVersion >= "1.6.20") {
        kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
    } else {
        kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable") // 1.3 thru 1.6.0 (not 1.6.20)
    }
}

// There is no *.java code in this project but newer Gradle complains if there
// is a mismatch with kotlin due to the runtime JDK being newer than kotlin target.
tasks.withType<JavaCompile> {
    targetCompatibility = "1.8"
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

    // ensure custom system properties are exposed to testing from CI
    System.getProperties().filter { it.key.toString().startsWith("org.unbrokendome.gradle.plugins.xjc") }
        .forEach { (t, u) -> systemProperty(t.toString(), u ?: "") }

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


fun resolveSystemGetenv(name: String, defaultValue: String? = null): String? {
    if(System.getenv().containsKey(name))
        return System.getenv(name)
    return defaultValue
}

val githubRepositoryOwner = resolveSystemGetenv("GITHUB_REPOSITORY_OWNER", "unbroken-dome")


pluginBundle {
    website = "https://github.com/${githubRepositoryOwner}/gradle-xjc-plugin"
    vcsUrl = "https://github.com/${githubRepositoryOwner}/gradle-xjc-plugin"
    description = "A plugin that integrates the XJC binding compiler into a Gradle build."
    tags = listOf("xjc", "jaxb", "code generation", "xml")

    (plugins) {
        "xjcPlugin" {
            displayName = "XJC Plugin"
        }
    }
}


apply {
    from("${rootDir}/publish.gradle.kts")
}
