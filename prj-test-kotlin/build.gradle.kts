/** 1) configure Gradle to grab plugin from output */
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


/** 2) use Gradle application plugin */
plugins {
    application
}


/** 3) apply UniformDependenciesPlugin */
apply(plugin = "com.thahnen.gradle.uniformdependencies")


/** 4) local variables */
group = "com.thahnen.gradle"
version = "1.0.0"
val className = "com.thahnen.gradle.prj_test_kotlin.PluginTest"


/** 5) configure dependencies repository */
repositories {
    mavenCentral()
}


/** 6) dependency configuration using the UniformDependenciesPlugin */
dependencies {
    // this notation must be used, see:
    // https://docs.gradle.org/current/userguide/kotlin_dsl.html#sec:kotlin_using_standard_api
    // or project.configurations.get("<Configuration>")(...)
    implementation("com.google.code.gson:gson:2.8.6")
    //"uImplementation"("com.google.code.gson:gson")
}


/** 7) configure application Gradle plugin */
application {
    mainClass.set(className)
}
