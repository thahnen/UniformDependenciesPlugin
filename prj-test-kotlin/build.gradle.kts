buildscript {
    repositories {
        flatDir {
            dirs("$projectDir/../build/libs")
        }
    }

    dependencies {
        classpath("com.thahnen.gradle:UniformDependenciesPlugin:1.0.0")
    }
}


plugins {
    kotlin("jvm") version "1.5.0"
}


apply(plugin = "com.thahnen.gradle.uniformdependencies")


group = "com.thahnen.gradle"
version = "1.0.0"


repositories {
    mavenCentral()
}


dependencies {
    //uImplementation("com.github.stefanbirkner:system-lambda")
    project.configurations.get("uImplementation")("com.github.stefanbirkner:system-lambda")
    project.configurations.get("uImplementation")("org.apache.logging.log4j:log4j-core")
}
