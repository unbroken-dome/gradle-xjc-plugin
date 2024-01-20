plugins {
    java
    id("org.unbroken-dome.xjc") version "2.2.0-SNAPSHOT"
}


repositories {
    mavenCentral()
}



xjc {
    // -Xann javax.annotation.Generated  could be  jakarta.annotation.Generated
    //   especially needed when xjcVersion = 3.0+ and targetVersion = 2.3
    extraArgs.addAll("-mark-generated", "-noDate", "-Xann", "javax.annotation.Generated")
}


dependencies {
    // 2.3 is the default xjcVersion
    implementation("javax.xml.bind:jaxb-api:2.3.1")

    // -mark-generated: means you need to add dependency:
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}
