plugins {
    `java-library`
    id("org.unbroken-dome.xjc") version "2.1.0-SNAPSHOT"
}


sourceSets.named("main") {
    xjcGenerateEpisode.set(true)
}


dependencies {
    api("javax.xml.bind:jaxb-api:2.3.0")
}
