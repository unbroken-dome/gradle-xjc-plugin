plugins {
    java
    id("org.unbroken-dome.xjc") version "2.1.0-SNAPSHOT"
}


repositories {
    mavenCentral()
}


sourceSets.named("main") {
    xjcExtraArgs.addAll("-Xequals", "-XhashCode", "-XtoString")
}


dependencies {
    implementation("javax.xml.bind:jaxb-api:2.3.0")

    "xjcClasspath"("org.jvnet.jaxb2_commons:jaxb2-basics:0.12.0")

    // The JAXB2 commons plugins require an additional compile/runtime dependency
    implementation("org.jvnet.jaxb2_commons:jaxb2-basics-runtime:0.12.0")
}
