plugins {
    java
    id("org.unbroken-dome.xjc") version "2.1.0-SNAPSHOT"
}


repositories {
    mavenCentral()
}


xjc {
    xjcVersion.set("2.4")
}


dependencies {
    // This is still needed for Gradle to compile the generated Java and add transitively
    implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
}
