/// build.gradle.kts (UniformDependenciesPlugin):
/// =============================================
///
/// Access gradle.properties:
///     yes -> "val prop_name = project.extra['prop.name']"
///     no  -> "val prop_name = property('prop.name')"


/** 1) Plugins used globally */
plugins {
    `kotlin-dsl`

    id("org.jetbrains.kotlin.jvm") version "1.4.20"
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
