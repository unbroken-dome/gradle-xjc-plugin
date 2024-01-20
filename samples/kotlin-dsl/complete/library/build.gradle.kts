plugins {
    `java-library`
    id("org.unbroken-dome.xjc") version "2.2.0-SNAPSHOT"
}


xjc {
    enableIntrospection.set(true)
}


sourceSets.named("main") {
    xjcGenerateEpisode.set(true)
    xjcExtraArgs.addAll("-Xequals", "-XhashCode", "-XtoString")
}


dependencies {
    api("javax.xml.bind:jaxb-api:2.3.0")

    "xjcClasspath"("org.jvnet.jaxb2_commons:jaxb2-basics:0.12.0")

    // The JAXB2 commons plugins require an additional compile/runtime dependency
    implementation("org.jvnet.jaxb2_commons:jaxb2-basics-runtime:0.12.0")
}


// Place the schema file into the JAR
tasks.named<Jar>("jar") {
    from("src/main/schema") {
        include("*.xsd")
        into("schema")
    }
}
