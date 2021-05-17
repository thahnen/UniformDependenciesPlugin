package com.thahnen.gradle

import org.gradle.api.InvalidUserDataException


/**
 *  Base extension for every extension thrown by this plugin
 *
 *  @author thahnen
 */
open class UniformDependenciesException(message: String) : InvalidUserDataException(message)


/**
 *  Exception thrown when no path to a properties file containing all dependencies could be found in either environment
 *  variables or (root) projects gradle.properties file
 */
open class MissingDependenciesPathException(message: String) : UniformDependenciesException(message)


/**
 *  Exception thrown when strictness level provided in environment variable or (root) projects gradle.properties file is
 *  not STRICT / LOOSELY / LOOSE
 */
open class WrongStrictnessLevelException(message: String) : UniformDependenciesException(message)


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
 *  Exception thrown when uniform configuration was called but a version was provided. This is not allowed because the
 *  actual version to use should be given in the corresponding property in properties file containing all dependencies
 *  which was provided to this plugin.
 */
open class VersionProvidedException(message: String) : UniformDependenciesException(message)


/**
 *  Exception thrown when artifact requested using one of the uniform dependency configurations was not found in
 *  properties file provided to the plugin via environment variables or (root) projects gradle.properties file
 */
open class DependencyNotFoundException(message: String) : UniformDependenciesException(message)
