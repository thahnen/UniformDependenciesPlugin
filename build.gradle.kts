/// build.gradle.kts (UniformDependenciesPlugin):
/// =============================================
///
/// Access gradle.properties:
///     yes -> "val prop_name = project.extra['prop.name']"
///     no  -> "val prop_name = property('prop.name')"


/** 1) Plugins used globally */
plugins {
    jacoco

    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`

    id("org.jetbrains.kotlin.jvm") version "1.4.20"
    id("com.gradle.plugin-publish") version "0.14.0"
    id("io.gitlab.arturbosch.detekt") version "1.19.0-RC2"
}


/** 2) General information regarding the plugin */
group   = project.extra["plugin.group"]!! as String
version = project.extra["plugin.version"]!! as String


/** 3) Dependency source configuration */
repositories {
    mavenCentral()
}


/** 4) Plugin dependencies */
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-bom")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("com.github.stefanbirkner:system-lambda:1.2.0")
    testImplementation(gradleTestKit())
}


/** 5) JaCoCo configuration */
jacoco {
    toolVersion = "0.8.6"
}

tasks.jacocoTestReport {
    reports {
        csv.isEnabled = true
    }
}


/** 6) detekt configuration */
detekt {
    ignoreFailures = true
    basePath = projectDir.toString()
}


/** 7) Plugin configuration */
pluginBundle {
    website = "https://github.com/thahnen/UniformDependenciesPlugin"
    vcsUrl  = "https://github.com/thahnen/UniformDependenciesPlugin.git"
    tags    = listOf("uniformity", "plugin management")
}


/** 8) Configuration for publishing plugin to Gradle Plugin Portal */
gradlePlugin {
    plugins {
        create(project.extra["plugin.name"]!! as String) {
            id                  = project.extra["plugin.id"]!! as String
            displayName         = project.extra["plugin.displayName"]!! as String
            description         = project.extra["plugin.description"]!! as String
            implementationClass = project.extra["plugin.class"]!! as String
        }
    }
}


/** 9) Configuration for publishing to private Maven Nexus repository */
System.getenv("PRIVATE_NEXUS_AVAILABLE")?.let {
    publishing {
        repositories {
            maven {
                url     = uri(project.extra["private.maven.nexus"]!!)
                name    = project.extra["private.maven.nexus.name"]!! as String

                credentials {
                    username = project.extra["private.maven.nexus.login.username"]!! as String
                    password = project.extra["private.maven.nexus.login.password"]!! as String
                }
            }
        }
    }
}
