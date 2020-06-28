plugins {
    kotlin("jvm") version "1.3.72"
    maven
}

group = "me.tgsc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.antlr:antlr4-runtime:4.8-1")
    compileOnly(files("csharp-1.0-SNAPSHOT.jar"))
}