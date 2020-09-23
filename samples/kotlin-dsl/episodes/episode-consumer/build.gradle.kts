plugins {
    java
    id("org.unbroken-dome.xjc") version "2.0.0"
}


dependencies {
    implementation(project(":episode-producer"))
    "xjcEpisodes"(project(":episode-producer"))
}
