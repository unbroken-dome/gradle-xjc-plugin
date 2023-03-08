plugins {
    `java-library`
    id("org.unbroken-dome.xjc") version "3.0.0"
}


sourceSets.named("main") {
    xjcGenerateEpisode.set(true)
}


dependencies {
    api("javax.xml.bind:jaxb-api:2.3.0")
}
