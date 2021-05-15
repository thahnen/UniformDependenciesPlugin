package com.thahnen.gradle

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
abstract class UniformDependenciesPluginExtension {

    /** stores the path to the properties file holding all information on dependencies provided to this plugin */
    abstract val path: Property<String>

    /** stores all dependencies seperated by ";" because I am too stupid to use the List<Property> data type -.- */
    abstract val dependencies: Property<String>
}
