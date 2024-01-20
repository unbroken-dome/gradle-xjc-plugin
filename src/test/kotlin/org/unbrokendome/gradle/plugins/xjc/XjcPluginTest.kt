package org.unbrokendome.gradle.plugins.xjc

import assertk.all
import assertk.assertThat
import assertk.assertions.isSuccess
import assertk.assertions.prop
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.unbrokendome.gradle.plugins.xjc.spek.applyPlugin
import org.unbrokendome.gradle.plugins.xjc.spek.setupGradleProject
import org.unbrokendome.gradle.plugins.xjc.testutil.assertions.*
import org.unbrokendome.gradle.plugins.xjc.testutil.evaluate
import org.unbrokendome.gradle.plugins.xjc.testutil.requiredExtension
import org.unbrokendome.gradle.plugins.xjc.testutil.sourceSets
import java.util.Locale


object XjcPluginTest : Spek({

    val project by setupGradleProject {
        applyPlugin<XjcPlugin>()
    }

    describe("applying the XJC plugin") {

        it("project can be evaluated successfully") {
            assertThat {
                project.evaluate()
            }.isSuccess()
        }


        it("should create an xjc DSL extension") {
            assertThat(project)
                .hasExtension<XjcExtension>(name = "xjc")
        }


        it("xjc DSL extension should apply defaults") {
            assertThat(project)
                .hasExtension<XjcExtension>().all {
                    this.prop(XjcExtension::xjcVersion).hasValueEqualTo(XjcExtension.DEFAULT_XJC_VERSION)
                    this.prop(XjcExtension::xjcVersionUnsupportedStrategy).hasValueEqualTo(XjcExtension.DEFAULT_XJC_VERSION_UNSUPPORTED_STRATEGY)
                    this.prop(XjcExtension::srcDirName).hasValueEqualTo(XjcExtension.DEFAULT_SRC_DIR_NAME)
                    this.prop(XjcExtension::encoding).hasValueEqualTo("UTF-8")
                    this.prop(XjcExtension::strictCheck).isTrue()
                    this.prop(XjcExtension::packageLevelAnnotations).isTrue()
                    this.prop(XjcExtension::noFileHeader).isTrue()
                    this.prop(XjcExtension::enableIntrospection).isFalse()
                    this.prop(XjcExtension::contentForWildcard).isFalse()
                    this.prop(XjcExtension::readOnly).isFalse()
                    this.prop(XjcExtension::extension).isFalse()
                }
        }


        it("should populate xjc DSL extension from project properties") {
            val extra = project.requiredExtension<ExtraPropertiesExtension>()
            extra.set("xjc.xjcVersion", "3.0")
            extra.set("xjc.xjcVersionUnsupportedStrategy", "default")
            extra.set("xjc.srcDirName", "xjc")
            extra.set("xjc.targetVersion", "2.2")
            extra.set("xjc.encoding", "ISO-8859-1")
            extra.set("xjc.docLocale", "it")
            extra.set("xjc.strictCheck", "false")
            extra.set("xjc.packageLevelAnnotations", "false")
            extra.set("xjc.noFileHeader", "false")
            extra.set("xjc.enableIntrospection", "true")
            extra.set("xjc.contentForWildcard", "true")
            extra.set("xjc.readOnly", "true")
            extra.set("xjc.extension", "true")

            assertThat(project)
                .hasExtension<XjcExtension>().all {
                    this.prop(XjcExtension::xjcVersion).hasValueEqualTo("3.0")
                    this.prop(XjcExtension::xjcVersionUnsupportedStrategy).hasValueEqualTo("default")
                    this.prop(XjcExtension::srcDirName).hasValueEqualTo("xjc")
                    this.prop(XjcExtension::targetVersion).hasValueEqualTo("2.2")
                    this.prop(XjcExtension::encoding).hasValueEqualTo("ISO-8859-1")
                    this.prop(XjcExtension::docLocale).hasValueEqualTo(Locale.ITALIAN.toString())
                    this.prop(XjcExtension::strictCheck).isFalse()
                    this.prop(XjcExtension::packageLevelAnnotations).isFalse()
                    this.prop(XjcExtension::noFileHeader).isFalse()
                    this.prop(XjcExtension::enableIntrospection).isTrue()
                    this.prop(XjcExtension::contentForWildcard).isTrue()
                    this.prop(XjcExtension::readOnly).isTrue()
                    this.prop(XjcExtension::extension).isTrue()
                }
        }


        it("should create global XJC configurations") {
            assertThat(project.configurations, "configurations").all {
                this.containsItem("xjcTool")
                this.containsItem("xjcClasspathGlobal")
                this.containsItem("xjcCatalogResolutionGlobal")
            }
        }
    }


    describe("source sets") {

        beforeEachTest {
            project.plugins.apply(JavaPlugin::class.java)
        }


        it("should create an XJC convention on each existing source set") {
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)

            assertThat(sourceSets).all {
                this.containsItem("main")
                    .hasConvention<XjcSourceSetConvention>()
                this.containsItem("test")
                    .hasConvention<XjcSourceSetConvention>()
            }
        }


        it("should create an XJC convention on each new source set") {
            val sourceSet = project.sourceSets.create("foo")
            assertThat(sourceSet, name = "sourceSet")
                .hasConvention<XjcSourceSetConvention>()
        }


        it("should create XJC configurations for each existing source set") {
            assertThat(project.configurations, name = "configurations").all {
                this.containsItem("xjcClasspath")
                    .extendsOnlyFrom("xjcClasspathGlobal")
                this.containsItem("xjcEpisodes")
                this.containsItem("xjcCatalogResolution").all {
                    extendsOnlyFrom("xjcCatalogResolutionGlobal", "compileClasspath")
                }

                this.containsItem("testXjcClasspath")
                    .extendsOnlyFrom("xjcClasspathGlobal")
                this.containsItem("testXjcEpisodes")
                this.containsItem("testXjcCatalogResolution").all {
                    extendsOnlyFrom("xjcCatalogResolutionGlobal", "testCompileClasspath")
                }
            }
        }


        it("should create XJC configurations for each new source set") {
            project.sourceSets.create("foo")
            assertThat(project.configurations, name = "configurations").all {
                this.containsItem("fooXjcClasspath")
                    .extendsOnlyFrom("xjcClasspathGlobal")
            }
        }
    }
})
