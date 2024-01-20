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
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")
}
