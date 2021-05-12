package com.thahnen.gradle


/**
 *  UniformDependenciesObject:
 *  =========================
 *
 *  internally used data class for storing dependencies parsed from properties file provided by user to plugin
 *
 *  @author Tobias Hahnen
 */
internal data class UniformDependenciesObject(var group: String, var name: String, var version: String)
