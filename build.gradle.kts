plugins {
    kotlin("jvm") version "1.3.72"
    maven
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "me.tgsc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.antlr:antlr4-runtime:4.8-1")
    implementation(files("csharp-1.0-SNAPSHOT.jar"))
}

tasks.named("install") {
    dependsOn("shadowJar")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}