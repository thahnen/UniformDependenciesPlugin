package com.thahnen.gradle

import java.io.File
import java.io.FileInputStream
import java.util.Properties

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.extra


/**
 *  UniformDependenciesPlugin:
 *  =========================
 *
 *  Plugin providing new configurations to use in dependencies block inside build.gradle / build.gradle.kts to match a
 *  dependency given by only its group and name with a version stored in properties file handed to the plugin using
 *  environment variable / in (root) projects gradle.properties file!
 *
 *  Result: - target.extra["plugins.uniformedependencies.path"] = <Path> is set
 *          - target.extra["<Group>:<Name>"] = <Version> is set for each dependency in provided properties file
 *
 *  @author Tobias Hahnen
 */
open class UniformDependenciesPlugin : Plugin<Project> {

    // identifiers of properties connected to this plugin
    private val KEY_PATH = "plugins.uniformdependencies.path"


    /** Overrides the abstract "apply" function */
    override fun apply(target: Project) {
        // 1) retrieve path to file containing all dependencies
        var propertiesPath = getPropertiesPath(target)

        // 2) resolve absolute path
        propertiesPath = resolveAbsolutePropertiesPath(target, propertiesPath)
        target.extra.set(KEY_PATH, propertiesPath)

        // 3) parse all dependencies from file at absolute path
        val dependencies = parseDependenciesList(propertiesPath)
        dependencies.forEach {
            target.extra.set("${it.group}:${it.name}", it.version)
        }

        // 4) apply Java plugin
        target.apply(JavaPlugin::class.java)

        // 5) add uniform dependency configuration for each configuration provided by Gradle + Java plugin
        createUniformDependencyConfiguration(target, "compileOnly")
        createUniformDependencyConfiguration(target, "implementation")
        createUniformDependencyConfiguration(target, "runtimeOnly")
        createUniformDependencyConfiguration(target, "testCompileOnly")
        createUniformDependencyConfiguration(target, "testImplementation")
        createUniformDependencyConfiguration(target, "testRuntimeOnly")

        // TODO: Maybe not add Java plugin and use dynamic version below to iterate over every configuration?
        // target.configurations.names.forEach {
        //     createUniformDependencyConfiguration(target, it)
        // }
    }


    /**
     *  Tries to retrieve the path to the file containing all dependencies (eg. "dependencies.properties") from
     *  - environment variable
     *  - gradle.properties in project
     *  - gradle.properties in root project
     *
     *  @param target the project which the plugin is applied to, may be sub-project
     *  @return given (relative, absolute?) path
     *  @throws MissingDependenciesPathException when path not found in environment variables or gradle.properties
     */
    @Throws(MissingDependenciesPathException::class)
    private fun getPropertiesPath(target: Project) : String {
        return System.getenv(KEY_PATH) ?: run {
            if (target.properties.containsKey(KEY_PATH)) {
                target.properties[KEY_PATH] as String
            } else if (target.rootProject.properties.containsKey(KEY_PATH)) {
                target.rootProject.properties[KEY_PATH] as String
            }

            throw MissingDependenciesPathException(
                "Path to properties file with all possible dependencies, marked with property identifier '$KEY_PATH' "
                + "not provided as environment variable or in (root) projects gradle.properties file!"
            )
        }
    }


    /**
     *  Tries to resolve the absolute path from path provided in environment variable / gradle.properties from
     *  - absolute path
     *  - path relative to project.projectDir
     *  - path relative to project.rootProject.projectDir
     *
     *  @param target the project which the plugin is applied to, may be sub-project
     *  @param path previously retrieved (relative, absolute?) path
     *  @return absolute path to properties file holding all dependencies
     *  @throws WrongDependenciesPathException path is not absolute and not relative to (root) project directory
     */
    @Throws(WrongDependenciesPathException::class)
    private fun resolveAbsolutePropertiesPath(target: Project, path: String) : String {
        // 1) Absolute path already given?
        var info = with(File(path)) {
            (exists() && isFile) t Pair(true, absolutePath) ?: Pair(false, null)
        }

        if (!info.first) {
            // 2) Relative path to target.projectDir given?
            info = with(File("${target.projectDir}/${path}")) {
                (exists() && isFile) t Pair(true, absolutePath) ?: Pair(false, null)
            }

            if (!info.first) {
                // 3) Relative path to target.rootProject.projectDir given?
                info = with(File("${target.rootProject.projectDir}/${path}")) {
                    (exists() && isFile) t Pair(true, absolutePath) ?: Pair(false, null)
                }

                if (!info.first) {
                    // Throw exception because path could not be found
                    throw WrongDependenciesPathException(
                        "Path to properties file with all possible dependencies, marked with property identifier "
                        + "'$KEY_PATH' could not be resolved to an absolute path or path relative to (root) project "
                        + "directory!"
                    )
                }
            }
        }

        return info.second!!
    }


    /**
     *  Parses given properties file containing all dependencies, where each dependency corresponds to two connected
     *  properties which look like
     *  - <Name>.group=<Group>
     *  - <Name>.version=<Version>
     *  because names are distinct but groups not
     *
     *  @param path previously resolved absolute path
     *  @return list of all dependencies found
     *  @throws ParsingDependenciesException when properties file is not correctly constructed
     */
    @Throws(ParsingDependenciesException::class)
    private fun parseDependenciesList(path: String) : List<UniformDependenciesObject> {
        val dependenciesObjects: List<UniformDependenciesObject> = emptyList()

        // load properties from given absolute path
        val properties = Properties()
        properties.load(FileInputStream(path))

        with(properties.propertyNames()) {
            if (this.toList().size % 2 != 0) {
                // Fehler, nur gerade Anzahl erlaubt!
                throw ParsingDependenciesException(
                    "$path is incorrectly constructed, number of properties provided must be even! This is because "
                    + "there are always two properties that make up a single dependency: <Name>.group=<Group> / "
                    + "<Name>.version=<Version>"
                )
            }

            while (this.hasMoreElements()) {
                // read two keys (fault tolerance)
                val key1 = this.nextElement() as String
                val key2 = this.nextElement() as String

                val dependency: UniformDependenciesObject

                if (key1.endsWith(".group") && key2.endsWith(".version")) {
                    if (key1.replace(".group", "") != key2.replace(".version", "")) {
                        throw ParsingDependenciesException(
                            "$path is incorrectly constructed, connected properties '$key1' and '$key2' do not "
                            + "correspond to the same dependency! This is because the property "
                            + "'${key1.replace(".group", "")}.version=<Version>' is missing or at the "
                            + "wrong place in the file!"
                        )
                    }

                    dependency = UniformDependenciesObject(
                        group   = properties[key1]!! as String,
                        name    = key1.replace(".group", ""),
                        version = properties[key2]!! as String
                    )
                } else if (key1.endsWith(".version") && key2.endsWith(".group")) {
                    if (key1.replace(".version", "") != key2.replace(".group", "")) {
                        throw ParsingDependenciesException(
                            "$path is incorrectly constructed, connected properties '$key1' and '$key2' do not "
                            + "correspond to the same dependency! This is because the property "
                            + "'${key1.replace(".version", "")}.group=<Group>' is missing or at the"
                            + "wrong place in the file!"
                        )
                    }

                    dependency = UniformDependenciesObject(
                        group   = key1.replace(".version", ""),
                        name    = properties[key2]!! as String,
                        version = properties[key1]!! as String
                    )
                } else {
                    // Fehler: Abhaengigkeiten muessen immer zusammenhaengend deklariert werden!
                    throw ParsingDependenciesException(
                        "$path is incorrectly constructed, properties '$key1' and '$key2' are both properties "
                        + "containing a group / version! There must always be two properties that make up a single "
                        + "dependency: <Name>.group=<Group> / <Name>.version=<Version>"
                    )
                }

                dependenciesObjects.plus(dependency)
            }
        }

        return dependenciesObjects
    }


    /**
     *  Creates a new dependency configuration based on the given configuration name
     *  No buildscript { dependencies { ... } } but dependencies { ... } configurations!
     *
     *  @param target the project which the plugin is applied to, may be sub-project
     *  @param configurationName name of the default(?) configuration
     *  @throws DependencyNotFoundException when dependency given in configuration was not provided using plugin
     */
    @Throws(DependencyNotFoundException::class)
    private fun createUniformDependencyConfiguration(target: Project, configurationName: String) {
        val (first: String, rest: String) = configurationName.splitAtIndex(1)
        val uniformConfigurationName = "u${first.toUpperCase()}$rest"

        // check if dependency configuration already exists
        target.configurations.findByName(uniformConfigurationName)?.let {
            return
        }

        target.dependencies.extra[uniformConfigurationName] = { group: String, name: String ->
            target.extra.get("$group:$name")?.let {
                target.dependencies.add(configurationName, mapOf(
                    "group" to group,
                    "name" to name,
                    "version" to (it as String)
                ))
            } ?: run {
                val propertiesPath = target.extra.get(KEY_PATH) ?: "(unknown*) dependencies properties file"

                throw DependencyNotFoundException(
                    "$uniformConfigurationName('$group:$name') could not be resolved to "
                    + "$configurationName('$group:$name:$<Version>') because dependency could not be found in "
                    + "$propertiesPath!"
                )
            }
        }
    }
}
