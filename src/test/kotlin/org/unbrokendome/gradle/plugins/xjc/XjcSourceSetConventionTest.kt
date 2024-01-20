package org.unbrokendome.gradle.plugins.xjc

import assertk.all
import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.unbrokendome.gradle.plugins.xjc.spek.applyPlugin
import org.unbrokendome.gradle.plugins.xjc.spek.setupGradleProject
import org.unbrokendome.gradle.plugins.xjc.testlib.directory
import org.unbrokendome.gradle.plugins.xjc.testutil.requiredConvention
import org.unbrokendome.gradle.plugins.xjc.testutil.requiredExtension
import org.unbrokendome.gradle.plugins.xjc.testutil.sourceSets


object XjcSourceSetConventionTest : Spek({

    val project by setupGradleProject {
        applyPlugin<JavaPlugin>()
        applyPlugin<XjcPlugin>()
    }


    describe("main source set") {

        val mainSourceSet by memoized {
            project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        }

        val xjcSourceSetConvention by memoized {
            (mainSourceSet as HasConvention).requiredConvention<XjcSourceSetConvention>()
        }


        it("should return the correct task and configuration names") {
            assertThat(xjcSourceSetConvention).all {
                this.prop(XjcSourceSetConvention::xjcGenerateTaskName)
                    .isEqualTo("xjcGenerate")
                this.prop(XjcSourceSetConvention::xjcClasspathConfigurationName)
                    .isEqualTo("xjcClasspath")
                this.prop(XjcSourceSetConvention::xjcEpisodesConfigurationName)
                    .isEqualTo("xjcEpisodes")
                this.prop(XjcSourceSetConvention::xjcCatalogResolutionConfigurationName)
                    .isEqualTo("xjcCatalogResolution")
            }
        }
    }


    describe("custom source set") {

        val sourceSet by memoized {
            project.sourceSets.create("custom")
        }

        val xjcSourceSetConvention by memoized {
            (sourceSet as HasConvention).requiredConvention<XjcSourceSetConvention>()
        }


        it("should return the correct task and configuration names") {
            assertThat(xjcSourceSetConvention).all {
                this.prop(XjcSourceSetConvention::xjcGenerateTaskName)
                    .isEqualTo("xjcGenerateCustom")
                this.prop(XjcSourceSetConvention::xjcClasspathConfigurationName)
                    .isEqualTo("customXjcClasspath")
                this.prop(XjcSourceSetConvention::xjcEpisodesConfigurationName)
                    .isEqualTo("customXjcEpisodes")
                this.prop(XjcSourceSetConvention::xjcCatalogResolutionConfigurationName)
                    .isEqualTo("customXjcCatalogResolution")
            }
        }


        it("should set default include filters") {
            assertThat(xjcSourceSetConvention).all {
                this.prop("xjcSchema") { it.xjcSchema }
                    .prop("includes") { it.includes }
                    .containsOnly("**/*.xsd")
                this.prop("xjcBinding") { it.xjcBinding }
                    .prop("includes") { it.includes }
                    .containsOnly("**/*.xjb")
                this.prop("xjcUrl") { it.xjcUrl }
                    .prop("includes") { it.includes }
                    .containsOnly("**/*.url")
            }
        }


        it("should honor the global xjcSrcDirName") {
            val xjc = project.requiredExtension<XjcExtension>()
            xjc.srcDirName.set("xjc")

            assertThat(xjcSourceSetConvention).all {
                this.prop("xjcSchema") { it.xjcSchema }
                    .prop("srcDirs") { it.srcDirs }
                    .containsOnly(project.file("src/custom/xjc"))
                this.prop("xjcBinding") { it.xjcBinding }
                    .prop("srcDirs") { it.srcDirs }
                    .containsOnly(project.file("src/custom/xjc"))
                this.prop("xjcUrl") { it.xjcUrl }
                    .prop("srcDirs") { it.srcDirs }
                    .containsOnly(project.file("src/custom/xjc"))
            }
        }


        it("should combine all source types in allSource") {
            directory(project.projectDir) {
                directory("src/custom/schema") {
                    file(name = "schema.xsd", contents = "")
                    file(name = "binding.xjb", contents = "")
                    file(name = "externals.url", contents = "")
                    file(name = "catalog.cat", contents = "")
                }
            }

            assertThat(sourceSet)
                .prop("allSource") { it.allSource }
                .containsAll(
                    project.file("src/custom/schema/schema.xsd"),
                    project.file("src/custom/schema/binding.xjb"),
                    project.file("src/custom/schema/externals.url"),
                    project.file("src/custom/schema/catalog.cat")
                )
        }
    }
})
