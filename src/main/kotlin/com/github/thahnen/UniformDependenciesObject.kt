package com.github.thahnen


/**
 *  Enumeration containing all possible strictness level for the configurations resolution strategy regarding all
 *  external dependencies
 *
 *  @author thahnen
 */
enum class Strictness {
    STRICT,
    LOOSELY,
    LOOSE
}


/**
 *  UniformDependenciesObject:
 *  =========================
 *
 *  internally used data class for storing dependencies parsed from properties file provided by user to plugin
 *
 *  @author thahnen
 */
internal data class UniformDependenciesObject(var group: String, var name: String, var version: String)
