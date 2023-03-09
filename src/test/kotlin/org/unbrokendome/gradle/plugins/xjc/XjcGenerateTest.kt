package org.unbrokendome.gradle.plugins.xjc

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPlugin
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.containsItem
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.dirValue
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.hasValueEqualTo
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.isFalse
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.isPresent
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.isTrue
import org.unbrokendome.gradle.plugins.xjc.spek.applyPlugin
import org.unbrokendome.gradle.plugins.xjc.spek.setupGradleProject
import org.unbrokendome.gradle.plugins.xjc.testlib.directory
import org.unbrokendome.gradle.plugins.xjc.testutil.requiredConvention
import org.unbrokendome.gradle.plugins.xjc.testutil.requiredExtension
import org.unbrokendome.gradle.plugins.xjc.testutil.sourceSets
import java.util.Locale


object XjcGenerateTest : Spek({

    val project by setupGradleProject {
        applyPlugin<XjcPlugin>()
    }


    describe("XjcGenerate task conventions") {

        val task by memoized {
            project.tasks.create("customXjcGenerate", XjcGenerate::class.java)
        }

        it("should use values from global xjc extension") {
            with(project.requiredExtension<XjcExtension>()) {
                targetVersion.set("XYZ")
                encoding.set("ISO-8859-1")
                docLocale.set(Locale.ITALIAN.toString())
                strictCheck.set(false)
                packageLevelAnnotations.set(false)
                noFileHeader.set(false)
                enableIntrospection.set(true)
                contentForWildcard.set(true)
                readOnly.set(true)
                extension.set(true)
            }

            assertThat(task, "task").all {
                prop(XjcGenerate::targetVersion).hasValueEqualTo("XYZ")
                prop(XjcGenerate::encoding).hasValueEqualTo("ISO-8859-1")
                prop(XjcGenerate::docLocale).hasValueEqualTo(Locale.ITALIAN.toString())
                prop(XjcGenerate::strictCheck).isFalse()
                prop(XjcGenerate::packageLevelAnnotations).isFalse()
                prop(XjcGenerate::noFileHeader).isFalse()
                prop(XjcGenerate::enableIntrospection).isTrue()
                prop(XjcGenerate::contentForWildcard).isTrue()
                prop(XjcGenerate::readOnly).isTrue()
                prop(XjcGenerate::extension).isTrue()
            }
        }


        it("should use the global tool classpath") {
            project.dependencies.add(
                "xjcTool",
                project.files("custom-xjc-1.2.3.jar")
            )

            assertThat(task, "task")
                .prop(XjcGenerate::toolClasspath)
                .containsOnly(project.file("custom-xjc-1.2.3.jar"))
        }


        it("should use the global extraArgs") {
            with(project.requiredExtension<XjcExtension>()) {
                extraArgs.add("-debug")
            }

            task.extraArgs.add("-verbose")

            assertThat(task, "task")
                .prop(XjcGenerate::extraArgs)
                .isPresent()
                .containsExactly("-debug", "-verbose")
        }
    }


    describe("XjcGenerate task from source set") {

        beforeEachTest {
            project.plugins.apply(JavaPlugin::class.java)
        }


        it("should create an XjcGenerate task for each existing source set") {
            assertThat(project.tasks, name = "tasks").all {
                containsItem("xjcGenerate")
                    .isInstanceOf(XjcGenerate::class)
                containsItem("xjcGenerateTest")
                    .isInstanceOf(XjcGenerate::class)
            }
        }


        it("should set input files from source set directories") {
            val task = project.tasks.getByName("xjcGenerate") as XjcGenerate

            directory(project.projectDir) {
                directory("src/main/schema") {
                    file(name = "schema.xsd", contents = "")
                    file(name = "binding.xjb", contents = "")
                    file(name = "externals.url", contents = "")
                    file(name = "catalog.cat", contents = "")
                }
            }

            assertThat(task, name = "task").all {
                prop(XjcGenerate::source)
                    .containsOnly(project.file("src/main/schema/schema.xsd"))
                prop(XjcGenerate::bindingFiles)
                    .containsOnly(project.file("src/main/schema/binding.xjb"))
                prop(XjcGenerate::urlSources)
                    .containsOnly(project.file("src/main/schema/externals.url"))
                prop(XjcGenerate::catalogs)
                    .containsOnly(project.file("src/main/schema/catalog.cat"))
            }
        }


        it("should set task classpaths from configurations") {
            val task = project.tasks.getByName("xjcGenerate") as XjcGenerate

            project.dependencies.add(
                "xjcClasspath", project.files("custom-jaxb-plugin-1.2.3.jar")
            )
            project.dependencies.add(
                "xjcEpisodes", project.files("custom-episode-1.2.3.jar")
            )

            assertThat(task, name = "task").all {
                prop(XjcGenerate::pluginClasspath)
                    .containsOnly(project.file("custom-jaxb-plugin-1.2.3.jar"))
                prop(XjcGenerate::episodes)
                    .containsOnly(project.file("custom-episode-1.2.3.jar"))
            }
        }


        it("should use the catalog resolution classpath") {
            val task = project.tasks.getByName("xjcGenerate") as XjcGenerate
            val catalogResolutionConfig = project.configurations.getByName("xjcCatalogResolution")

            assertThat(task, name = "task").all {
                prop(XjcGenerate::catalogResolutionClasspath)
                    .hasValueEqualTo(catalogResolutionConfig)
            }
        }


        it("should set task properties from source set") {
            val mainSourceSet = project.sourceSets.getByName("main")
            val xjcSourceSetConvention =
                (mainSourceSet as HasConvention).requiredConvention<XjcSourceSetConvention>()
            val task = project.tasks.getByName("xjcGenerate") as XjcGenerate

            xjcSourceSetConvention.xjcTargetPackage.set("com.example")
            xjcSourceSetConvention.xjcGenerateEpisode.set(true)

            assertThat(task, name = "task").all {
                prop(XjcGenerate::targetPackage)
                    .hasValueEqualTo("com.example")
                prop(XjcGenerate::generateEpisode)
                    .hasValueEqualTo(true)
            }
        }


        it("should add extra args from global xjc extension and from source set") {

            with(project.requiredExtension<XjcExtension>()) {
                extraArgs.add("-extra1")
            }

            val mainSourceSet = project.sourceSets.getByName("main")
            val xjcSourceSetConvention =
                (mainSourceSet as HasConvention).requiredConvention<XjcSourceSetConvention>()
            xjcSourceSetConvention.xjcExtraArgs.add("-extra2")

            val task = project.tasks.getByName("xjcGenerate") as XjcGenerate
            task.extraArgs.add("-extra3")

            assertThat(task, name = "task")
                .prop(XjcGenerate::extraArgs)
                .isPresent()
                .containsExactly("-extra1", "-extra2", "-extra3")
        }


        it("should set output directories") {
            val task = project.tasks.getByName("xjcGenerate") as XjcGenerate

            assertThat(task, name = "task").all {
                prop(XjcGenerate::outputDirectory)
                    .dirValue().isEqualTo(project.file("build/generated/sources/xjc/java/main"))
                prop(XjcGenerate::episodeOutputDirectory)
                    .dirValue().isEqualTo(project.file("build/generated/resources/xjc/main"))
            }
        }
    }
})
