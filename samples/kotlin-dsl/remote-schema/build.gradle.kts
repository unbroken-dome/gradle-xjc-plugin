plugins {
    java
    id("org.unbroken-dome.xjc") version "2.0.0"
}


repositories {
    jcenter()
}


dependencies {
    implementation("javax.xml.bind:jaxb-api:2.3.0")
}
