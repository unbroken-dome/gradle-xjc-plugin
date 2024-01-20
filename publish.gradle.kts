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



            if(findProperty("doGitHubPackagesPublish") == "true") {
                val GITHUB_ACTOR = System.getenv("GITHUB_USERNAME") ?: System.getenv("GITHUB_ACTOR") ?: throw IllegalArgumentException()
                val GITHUB_TOKEN = System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN") ?: throw IllegalArgumentException()
                val GITHUB_REPOSITORY = System.getenv("GITHUB_REPOSITORY")

                assert(GITHUB_ACTOR.isNotEmpty() == true)
                assert(GITHUB_TOKEN.isNotEmpty() == true)
                assert(GITHUB_REPOSITORY.isNotEmpty() == true)

                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/${GITHUB_REPOSITORY}")
                    credentials {
                        username = GITHUB_ACTOR
                        password = GITHUB_TOKEN
                    }
                }
            }
        }
    }
}

