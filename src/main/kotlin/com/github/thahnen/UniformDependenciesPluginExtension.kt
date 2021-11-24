package com.github.thahnen

import org.gradle.api.provider.Property


/**
 *  UniformDependenciesPluginExtension:
 *  ==================================
 *
 *  Extension to this plugin but not for configuration, only for storing data as project.ext / project.extra is not
 *  available when working with the configurations resolution strategy for dependencies
 *
 *  @author thahnen
 */
@Suppress("UnnecessaryAbstractClass")
abstract class UniformDependenciesPluginExtension {

    /** stores the path to the properties file holding all information on dependencies provided to this plugin */
    abstract val path: Property<String>

    /** stores the strictness value: STRICT -> always throw exception / LOOSELY -> throw exception on direct
     *  dependencies / LOOSE -> no exception only warning */
    abstract val strictness: Property<Strictness>

    /** stores all dependencies seperated by ";" because I am too stupid to use the List<Property> data type -.- */
    abstract val dependencies: Property<String>
}
