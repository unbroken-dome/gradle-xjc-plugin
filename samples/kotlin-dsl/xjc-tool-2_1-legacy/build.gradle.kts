plugins {
    java
    id("org.unbroken-dome.xjc") version "2.1.0-SNAPSHOT"
}


repositories {
    mavenCentral()
}


xjc {
    xjcVersion.set("2.1")
}


dependencies {
    // These legacy jars do not have any OSGi MANIFEST data,
    //   that is what makes them legacy as plugin auto-detect works slightly different
    implementation("javax.xml.bind:jaxb-api:2.1")

    xjcTool("javax.xml.bind:jaxb-api:2.1")

    xjcTool("com.sun.xml.bind:jaxb-xjc:2.1.17")
    xjcTool("com.sun.xml.bind:jaxb-impl:2.1.17")
    xjcTool("com.sun.xml.bind:jaxb-core:2.1.14")
}
