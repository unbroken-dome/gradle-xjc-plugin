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
    // This is still needed for Gradle to compile the generated Java and add transitively
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
}
