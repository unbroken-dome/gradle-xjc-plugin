plugins {
    java
    id("org.unbroken-dome.xjc") version "2.2.0-SNAPSHOT"
}


repositories {
    mavenCentral()
}


xjc {
    xjcVersion.set("2.4")
}


dependencies {
    implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
    //implementation("javax.xml.bind:jaxb-api:2.3.1")
    //implementation("javax.xml.bind:jaxb-api:2.2.12")

    xjcTool("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")

    xjcTool("com.sun.xml.bind:jaxb-xjc:2.4.0-b180830.0438")
    xjcTool("com.sun.xml.bind:jaxb-impl:2.4.0-b180830.0438")
    xjcTool("com.sun.xml.bind:jaxb-core:2.3.0.1")  // there is no 2.4 version
}
