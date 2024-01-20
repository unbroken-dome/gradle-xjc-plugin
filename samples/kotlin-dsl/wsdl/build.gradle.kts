plugins {
    java
    id("org.unbroken-dome.xjc") version "2.2.0-SNAPSHOT"
}


repositories {
    mavenCentral()
}

xjc {
    // This maybe inferred from the filename extension provided to XJC
    extraArgs.addAll("-wsdl")
}

// With wsimport there is:
//  cd src/main/schema
//  mkdir out
//  wsimport -verbose -keep -extension -b books.xjb -d out -wsdllocation "http://localhost/book/lookup/service" -Xdebug -XdisableAuthenticator books.wsdl
// But this is outside the scope of XJB see other gradle plugins for wsimport support

// XJC seems able to produce JAXB for *.wsdl and *.xsd but it includes all eligible
//   file extensions from src/main/schema/** so when imports are used it appears to
//   need those file to be relocated outside of src/main/schema so this is the reason
//   for the schema_other directory

dependencies {
    implementation("javax.xml.bind:jaxb-api:2.3.0")
}
