plugins {
    java
    id("org.unbroken-dome.xjc") version "3.0.0"
}


xjc {
    extension.set(true)
}


dependencies {
    implementation("javax.xml.bind:jaxb-api:2.3.0")
    "xjcCatalogResolution"(project(":schema-library"))
}
