plugins {
    java
    id("org.unbroken-dome.xjc") version "2.1.0-SNAPSHOT"
}


repositories {
    mavenCentral()
}


xjc {
    xjcVersion.set("4.0")
}


dependencies {
    // XJC 3.0 requires a different JAXB API artifact
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")

    xjcTool("com.sun.xml.bind:jaxb-xjc:4.0.2")
}
