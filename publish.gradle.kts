import org.gradle.api.publish.PublishingExtension

allprojects {
    apply(plugin = "maven-publish")

    tasks.withType<Javadoc>().configureEach {
        options {
            this as StandardJavadocDocletOptions

            if(JavaVersion.current().isJava8Compatible()) {
                addStringOption("Xdoclint:none", "-quiet")
            }
            if(JavaVersion.current().isJava9Compatible()) {
                addBooleanOption("html5", true)
            }
        }
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                // Default is Maven buildDirectory publish only
                url = uri(layout.buildDirectory.dir("repo"))
            }
        }
    }
}
