# UniformDependenciesPlugin

![example workflow](https://github.com/thahnen/UniformDependenciesPlugin/actions/workflows/gradle.yml/badge.svg)
[![Coverage](.github/badges/jacoco.svg)](https://github.com/thahnen/UniformDependenciesPlugin/actions/workflows/gradle.yml)

Gradle plugin for using uniform dependencies across multiple (sub-) projects.

## Usage

To use this plugin you need to provide a properties file containing dependencies to the project. You can use
environment variables or the (root) projects gradle.properties file using with the following scheme:

```properties
# Path to a properties file containing all dependencies
plugins.uniformdependencies.path=/var/project/dependencies.properties

# Which mode should be used when evaluating dependencies (STRICT / LOOSELY / LOOSE)
plugins.uniformdependencies.strictness=STRICT
```

### Dependencies properties file

The properties file provided to the plugin has to look like this:

```properties
# Scheme of an external artifact:
# 1) <Name>.group=<Group>
# 2) <Name>.version=<Version>

# 1. example used with 'implementation("com.github.stefanbirkner:system-lambda")'
system-lambda.group=com.github.stefanbirkner
system-lambda.version=1.2.0

# 2. example used with 'implementation("org.apache.logging.log4j:log4j-core")'
log4j-core.version=2.14.1
log4j-core.group=org.apache.logging.log4j
```

When declaring external dependencies and it is mentioned in the properties file but used with a version, the plugin
will throw an exception because resolving the version is only done by itself!
