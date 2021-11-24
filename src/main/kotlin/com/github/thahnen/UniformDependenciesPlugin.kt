package com.github.thahnen

import java.io.File
import java.io.FileInputStream
import java.util.Properties

import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra


/**
 *  UniformDependenciesPlugin:
 *  =========================
 *
 *  Plugin providing a new resolution strategy for all configurations found to match dependencies listed in a specific
 *  properties file provided to this plugin to the ones set using configurations like "implementation(...)" to always
 *  use the correct version!
 *
 *  Result: - target.extensions.getByType(UniformDependenciesPluginExtension::class.java) for the following properties
 *          - path          -> path to properties file provided to this plugin
 *          - dependencies  -> all dependencies listed as one simple string
 *
 *  @author thahnen
 */
open class UniformDependenciesPlugin : Plugin<Project> {

    companion object {
        // identifiers of properties connected to this plugin
        internal const val KEY_PATH         = "plugins.uniformdependencies.path"
        internal const val KEY_STRICTNESS   = "plugins.uniformdependencies.strictness"
        internal const val KEY_EXTENSION    = "uniformdependenciespluginextension"


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
        internal fun parseDependenciesList(path: String) : MutableList<UniformDependenciesObject> {
            val dependenciesObjects: MutableList<UniformDependenciesObject> = mutableListOf()

            // load properties from given absolute path
            val properties = Properties()
            properties.load(FileInputStream(path))

            val list = properties.stringPropertyNames().toList()
            if (list.isEmpty()) {
                throw ParsingDependenciesException(
                    "$path is incorrectly constructed, no dependency found (possible comments were ignored)!"
                )
            } else if (list.size % 2 != 0) {
                throw ParsingDependenciesException(
                    "$path is incorrectly constructed, number of properties provided must be even! This is because "
                    + "there are always two properties that make up a single dependency: <Name>.group=<Group> / "
                    + "<Name>.version=<Version>"
                )
            }

            var i = 0
            while (i < list.size) {
                val key1 = list[i++]
                val key2 = list[i++]

                val dependency: UniformDependenciesObject

                if (key1.endsWith(".group") && key2.endsWith(".version")) {
                    if (key1.replace(".group", "") != key2.replace(".version", "")) {
                        throw ParsingDependenciesException(
                            "$path is incorrectly constructed, connected properties '$key1' and '$key2' do not "
                            + "correspond to the same dependency! This is because the property "
                            + "'${key1.replace(".group", "")}.version=<Version>' is missing or at "
                            + "the wrong place in the file!"
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
                            + "'${key1.replace(".version", "")}.group=<Group>' is missing or at the "
                            + "wrong place in the file!"
                        )
                    }

                    dependency = UniformDependenciesObject(
                        group   = properties[key2]!! as String,
                        name    = key1.replace(".version", ""),
                        version = properties[key1]!! as String
                    )
                } else {
                    throw ParsingDependenciesException(
                        "$path is incorrectly constructed, properties '$key1' and '$key2' are both properties "
                        + "containing a group / version! There must always be two properties that make up a single "
                        + "dependency: <Name>.group=<Group> / <Name>.version=<Version>"
                    )
                }

                dependenciesObjects.add(dependency)
            }

            return dependenciesObjects
        }
    }


    /** Overrides the abstract "apply" function */
    override fun apply(target: Project) {
        // 1) retrieve path to file containing all dependencies
        var propertiesPath = getPropertiesPath(target)

        // 2) retrieve strictness level
        val strictness = getStrictnessLevel(target)

        // 3) resolve absolute path
        propertiesPath = resolveAbsolutePropertiesPath(target, propertiesPath)

        // 4) parse all dependencies from file at absolute path
        val dependencies = parseDependenciesList(propertiesPath)

        var dependenciesString = ""
        dependencies.forEach {
            dependenciesString += ("${it.group}:${it.name}:${it.version};")
        }
        dependenciesString = dependenciesString.substring(0, dependenciesString.length-1)

        // 5) custom extension to store tha data (as target.extra could not be used in resolution of dependencies)
        val extension = target.extensions.create<UniformDependenciesPluginExtension>(KEY_EXTENSION)
        extension.path.set(propertiesPath)
        extension.strictness.set(strictness)
        extension.dependencies.set(dependenciesString)

        // 6) apply Java plugin
        target.apply(plugin = "java")

        // 7) change resolution strategy of all configurations to check for uniform dependencies
        //    TODO: Implement checking on Strictness other than dependency (not) found in properties file!
        target.configurations.all {
            // get configuration name
            val name = this.name

            resolutionStrategy {
                eachDependency {
                    val dependency = this.requested

                    var dependencyString: String? = null
                    extension.dependencies.get().split(";").forEach {
                        if (it.startsWith("${dependency.group}:${dependency.name}")) {
                            dependencyString = it
                            return@forEach
                        }
                    }

                    dependencyString?.let {
                        val version = it.split("${dependency.group}:${dependency.name}:")[1]

                        if (dependency.version.isNullOrBlank() || dependency.version!!.trim().isBlank()) {
                            useVersion(version)
                            because(
                                "Dependency specified in properties file containing all dependencies "
                                + "provided to this plugin!"
                            )
                            return@let
                        }

                        throw VersionProvidedException(
                            "${dependency.group}:${dependency.name} was found in properties file containing all "
                            + "dependencies provided to this plugin. Therefore providing a version is not allowed, "
                            + "please change this dependency configuration to:\n"
                            + "${name}(\"${dependency.group}:${dependency.name}\")"
                        )
                    } ?: run {
                        val path = target.extra.get(KEY_PATH) ?: "(unknown*) dependencies properties file"

                        if (extension.strictness.get() == Strictness.STRICT) {
                            throw DependencyNotFoundException(
                                "${dependency.group}:${dependency.name} was not found in properties file $path "
                                 + "containing all dependencies provided to this plugin. Therefore it must be set "
                                + "there! Exception thrown because strictness level set to Strictness.STRICT!"
                            )
                        }

                        println(
                            "[UniformDependenciesPlugin] $path does not contain this dependency "
                            + "'${dependency.group}:${dependency.name}:${dependency.version}'. Maybe consider adding "
                            + "it or ignore this warning IF IT IS a dependency of another dependency! No exception "
                            + "thrown because strictness level is not set to Strictness.STRICT!"
                        )
                    }
                }
            }
        }
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
            } else {
                throw MissingDependenciesPathException(
                    "Path to properties file with all possible dependencies, marked with property identifier "
                    + "'$KEY_PATH' not provided as environment variable or in (root) projects gradle.properties file!"
                )
            }
        }
    }


    /**
     *  Tries to retrieve the strictness level from
     *  - environment variable
     *  - gradle.properties in project
     *  - gradle.properties in root project
     *
     *  @param target the project which the plugin is applied to, may be sub-project
     *  @return strictness level from one of the sources or fallback (Strictness.LOOSELY)
     *  @throws WrongStrictnessLevelException when strictness level given is not STRICT / LOOSELY / LOOSE
     */
    @Throws(WrongStrictnessLevelException::class)
    private fun getStrictnessLevel(target: Project) : Strictness {
        return System.getenv(KEY_STRICTNESS)?.let {
            try {
                Strictness.valueOf(it)
            } catch (ignored: IllegalArgumentException) {
                throw WrongStrictnessLevelException(
                    "Strictness level provided by environment variable '${it}' was incorrect! Possible values are: "
                    + Strictness.values().toString()
                )
            }
        } ?: run {
            try {
                if (target.properties.containsKey(KEY_STRICTNESS)) {
                    Strictness.valueOf(target.properties[KEY_STRICTNESS] as String)
                } else if (target.rootProject.properties.containsKey(KEY_STRICTNESS)) {
                    Strictness.valueOf(target.rootProject.properties[KEY_STRICTNESS] as String)
                } else {
                    Strictness.LOOSELY
                }
            } catch (ignored: IllegalArgumentException) {
                throw WrongStrictnessLevelException(
                    "Strictness level provided in gradle.properties was incorrect! Possible values are: "
                    + Strictness.values().toString()
                )
            }
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
}


/**
 *  Extension to Boolean to create a ternary operator
 *  -> <Condition> ? <to do if true> ?: <to do if false>
 */
internal infix fun <T: Any> Boolean.t(value: T) : T? = if (this) value else null
