package com.thahnen.gradle

import java.io.File
import java.io.FileInputStream
import java.util.*

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.kotlin.dsl.extra


/**
 *  UniformDependenciesPlugin:
 *  =========================
 *
 *  @author Tobias Hahnen
 */
open class UniformDependenciesPlugin : Plugin<Project> {

    // identifiers of properties connected to this plugin
    private val KEY_PATH    = "plugins.uniformdependencies.path"


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

        // 4) set extension to make "resolve" function visible
        target.convention.plugins["uniformdependencies"] = UniformDependenciesExtension()
    }


    /**
     *  Tries to retrieve the path to the file containing all dependencies (eg. "dependencies.properties") from
     *  - environment variable
     *  - gradle.properties in project
     *  - gradle.properties in root project
     *
     *  @param target the project which the plugin is applied to, may be sub-project
     *  @return given (relative, absolute?) path
     *  @throws MissingDependenciesPathException when path not found in environment nor gradle.properties
     */
    @Throws(MissingDependenciesPathException::class)
    private fun getPropertiesPath(target: Project) : String {
        return System.getenv(KEY_PATH) ?: run {
            if (target.properties.containsKey(KEY_PATH)) {
                target.properties[KEY_PATH] as String
            } else if (target.rootProject.properties.containsKey(KEY_PATH)) {
                target.rootProject.properties[KEY_PATH] as String
            }

            throw MissingDependenciesPathException("")
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
     *  @throws MissingDependenciesPathException when absolute path could not be found
     */
    @Throws(MissingDependenciesPathException::class)
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
                    throw MissingDependenciesPathException("")
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
    private fun parseDependenciesList(path: String) : List<UniformDependency> {
        val dependencies: List<UniformDependency> = emptyList()

        val properties = Properties()
        properties.load(FileInputStream(path))

        with(properties.propertyNames()) {
            if (this.toList().size % 2 != 0) {
                // Fehler, nur gerade Anzahl erlaubt!
                throw ParsingDependenciesException("")
            }

            while (this.hasMoreElements()) {
                val group: String
                val name: String
                val version: String

                val key1 = this.nextElement() as String
                val key2 = this.nextElement() as String
                if (key1.endsWith(".group") && key2.endsWith(".version")) {
                    name    = key1.replace(".group", "")
                    group   = properties[key1]!! as String
                    version = properties[key2]!! as String
                } else if (key1.endsWith(".version") && key2.endsWith(".group")) {
                    name    = key1.replace(".version", "")
                    group   = properties[key2]!! as String
                    version = properties[key1]!! as String
                } else {
                    // Fehler: Abhaengigkeiten muessen immer zusammenhaengend deklariert werden!
                    throw ParsingDependenciesException("")
                }

                dependencies.plus(UniformDependency(group, name, version))
            }
        }

        return dependencies
    }


    /**
     *  UniformDependenciesExtension:
     *  ============================
     *
     *  "extension" provided by UniformDependenciesExtension to allow using the "resolve" function when devlaring
     *  dependencies like:
     *
     *      dependencies {
     *          implementation(resolve(project, "com.thahnen.gradle", "UniformDependenciesExtension")
     *
     *          // ... translates to ...
     *
     *          implementation("com.thahnen.gradle:UniformDependenciesPlugin:1.0.0")
     *      }
     */
    class UniformDependenciesExtension {

        /**
         *  Function which resolves a given artifact group and name with version provided in properties file containing
         *  all dependencies to complete dependency string: "${group}:${name}:${version}"
         *
         *  @param project
         *  @param group
         *  @param name
         *  @return
         *  @throws DependencyNotFoundException
         */
        @Throws(DependencyNotFoundException::class)
        fun resolve(project: Project, group: String, name: String) : Map<String, String> {
            project.extra.get("${group}:${name}")?.let {
                return mapOf(
                    "group" to group,
                    "name" to name,
                    "version" to (it as String)
                )
            } ?: run {
                throw DependencyNotFoundException("")
            }
        }
    }
}


/**
 *  Base extension for every extension thrown by this plugin
 */
open class UniformDependenciesPluginException(message: String) : InvalidUserDataException(message)

open class MissingDependenciesPathException(message: String) : UniformDependenciesPluginException(message)

open class ParsingDependenciesException(message: String) : UniformDependenciesPluginException(message)

open class DependencyNotFoundException(message: String) : UniformDependenciesPluginException(message)


infix fun <T: Any> Boolean.t(value: T) : T? = if (this) value else null
