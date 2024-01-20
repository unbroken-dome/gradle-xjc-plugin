plugins {
    java
    id("org.unbroken-dome.xjc") version "2.2.0-SNAPSHOT"
}


repositories {
    mavenCentral()
}


dependencies {
    implementation("javax.xml.bind:jaxb-api:2.3.0")
}
