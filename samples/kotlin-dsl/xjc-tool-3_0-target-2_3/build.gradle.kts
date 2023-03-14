plugins {
    java
    id("org.unbroken-dome.xjc") version "2.1.0-SNAPSHOT"
}


repositories {
    mavenCentral()
}


xjc {
    xjcVersion.set("3.0")
    targetVersion.set("2.3")
}


dependencies {
    // Yes this looks wrong, based on XJC command line usage:
    //    XJC 3.x/4.x itself does not need javax.xml.bind on its classpath,
    //       it needs jakarta.xml.bind for generation.
    //    JavaC does need to see javax.xml.bind as that is what is in the generated *.java

    implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")

    xjcTool("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")

    xjcTool("com.sun.xml.bind:jaxb-xjc:3.0.2")
}
