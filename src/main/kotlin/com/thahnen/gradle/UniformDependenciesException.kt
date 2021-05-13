package com.thahnen.gradle

import org.gradle.api.InvalidUserDataException


/**
 *  Base extension for every extension thrown by this plugin
 *
 *  @author Tobias Hahnen
 */
open class UniformDependenciesException(message: String) : InvalidUserDataException(message)


/**
 *  Exception thrown when no path to a properties file containing all dependencies could be found in either environment
 *  variables or (root) projects gradle.properties file
 */
open class MissingDependenciesPathException(message: String) : UniformDependenciesException(message)


/**
 *  Exception thrown when path to properties file containing all dependencies provided via environment variables or
 *  (root) projects gradle.properties file was neither absolute nor relative to (root) project directory
 */
open class WrongDependenciesPathException(message: String) : UniformDependenciesException(message)


/**
 *  Exception thrown when parsing the properties file containing all dependencies is not possible due to it being
 *  wrongly constructed (eg. wrong order, missing property, ...)
 */
open class ParsingDependenciesException(message: String) : UniformDependenciesException(message)


/**
 *  Exception thrown when creating a uniform configuration name from given configuration name could not be done because
 *  given configuration does not exist in project
 */
open class ConfigurationNotFoundException(message: String) : UniformDependenciesException(message)


/**
 *  Exception thrown when artifact requested using one of the uniform dependency configurations was not found in
 *  properties file provided to the plugin via environment variables or (root) projects gradle.properties file
 */
open class DependencyNotFoundException(message: String) : UniformDependenciesException(message)
