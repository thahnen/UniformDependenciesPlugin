package com.thahnen.gradle

import org.junit.Before
import org.junit.Test

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

import kotlin.test.assertEquals

import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder

import org.gradle.kotlin.dsl.extra

import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable


/**
 *  UniformDependenciesPluginTest:
 *  =============================
 *
 *  jUnit test cases on the UniformDependenciesPlugin
 */
class UniformDependenciesPluginTest {

    // test cases properties file
    private val dependenciesPropertiesPath: String = this.javaClass.classLoader.getResource("dependencies.properties")!!
                                                        .path.replace("%20", " ")

    // test cases properties
    private val dependenciesProperties = Properties()


    /** 0) Configuration to read properties once before running multiple tests using them */
    @Throws(IOException::class)
    @Before fun configureTestsuite() {
        dependenciesProperties.load(FileInputStream(dependenciesPropertiesPath))
    }


    /** 1) Tests only applying the plugin (without environment variable / project properties used for configuration) */
    @Test fun testApplyPluginWithoutPropertiesToProject() {
        val project = ProjectBuilder.builder().build()

        try {
            // try applying plugin (should fail)
            project.pluginManager.apply(UniformDependenciesPlugin::class.java)
        } catch (e: Exception) {
            // assert applying did not work
            // INFO: equal to check on InvalidUserDataException as it is based on it
            assert(e.cause is UniformDependenciesException)
        }

        assertEquals(false, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
    }


    /** 2) Tests only applying the plugin (with environment variable used for configuration) */
    @Test fun testApplyPluginWithEnvironmentVariableToProject() {
        val project = ProjectBuilder.builder().build()

        withEnvironmentVariable(
            "plugins.uniformdependencies.path", dependenciesPropertiesPath
        ).execute {
            // assert the environment variable is set correctly
            assertEquals(dependenciesPropertiesPath, System.getenv("plugins.uniformdependencies.path"))

            // apply plugin
            project.pluginManager.apply(UniformDependenciesPlugin::class.java)

            // assert that plugin is loaded
            assertEquals(true, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }
    }


    /** 3) Tests only applying the plugin (with project gradle.properties used for configuration) */
    @Test fun testApplyPluginWithGradlePropertiesToProject() {
        val project = ProjectBuilder.builder().build()

        // project gradle.properties reference (project.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        propertiesExtension["plugins.uniformdependencies.path"] = dependenciesPropertiesPath

        // apply plugin
        project.pluginManager.apply(UniformDependenciesPlugin::class.java)

        // assert that plugin is loaded
        assertEquals(true, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
    }


    /** 4) Tests applying the plugin and evaluates the extra properties set by plugin */
    @Test fun testEvaluateCorrectnessExtraProperties() {
        val project = ProjectBuilder.builder().build()

        // project gradle.properties reference (project.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        propertiesExtension["plugins.uniformdependencies.path"] = dependenciesPropertiesPath

        // apply plugin
        project.pluginManager.apply(UniformDependenciesPlugin::class.java)

        // assert that path to dependencies.properties is saved in project extra property
        assertEquals(true, project.extra.has("plugins.uniformdependencies.path"))
        assertEquals(File(dependenciesPropertiesPath).absolutePath, project.extra["plugins.uniformdependencies.path"])

        // assert that all dependencies from dependencies.properties are saved in project extra property
        UniformDependenciesPlugin.parseDependenciesList(File(dependenciesPropertiesPath).absolutePath).forEach {
            assertEquals(true, project.extra.has("${it.group}:${it.name}"))
            assertEquals(it.version, project.extra["${it.group}:${it.name}"])
        }
    }


    /** 5) Tests applying the plugin and evaluates that Java plugin was applied */
    @Test fun testEvaluateCorrectnessJavaPlugin() {
        val project = ProjectBuilder.builder().build()

        // project gradle.properties reference (project.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        propertiesExtension["plugins.uniformdependencies.path"] = dependenciesPropertiesPath

        // apply plugin
        project.pluginManager.apply(UniformDependenciesPlugin::class.java)

        // assert that Java plugin was applied to the project
        assertEquals(true, project.plugins.hasPlugin(JavaPlugin::class.java))
    }


    /** 6) Tests applying the plugin and evaluates uniform dependencies configurations */
    @Test fun testEvaluateUniformDependenciesConfigurations() {
        val project = ProjectBuilder.builder().build()

        // project gradle.properties reference (project.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        propertiesExtension["plugins.uniformdependencies.path"] = dependenciesPropertiesPath

        // apply plugin
        project.pluginManager.apply(UniformDependenciesPlugin::class.java)

        // assert that new uniform dependencies configuration exist & they extend the old dependencies configuration
        listOf(
            "compileOnly", "implementation", "runtimeOnly", "testCompileOnly", "testImplementation", "testRuntimeOnly"
        ).forEach {
            val configuration = project.configurations.getByName(
                UniformDependenciesPlugin.createUniformConfigurationName(project, it)
            )

            assertEquals(true, configuration.extendsFrom.contains(project.configurations.getByName(it)))
        }
    }
}
