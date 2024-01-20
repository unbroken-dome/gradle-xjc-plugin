plugins {
    java
    id("org.unbroken-dome.xjc") version "2.2.0-SNAPSHOT"
}


repositories {
    mavenCentral()
}


xjc {
    xjcVersion.set("3.0")
}


dependencies {
    // This is still needed for Gradle to compile the generated Java and add transitively
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")
}
