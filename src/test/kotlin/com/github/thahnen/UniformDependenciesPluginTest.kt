package com.github.thahnen

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder

import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable


/**
 *  UniformDependenciesPluginTest:
 *  =============================
 *
 *  jUnit test cases on the UniformDependenciesPlugin
 */
class UniformDependenciesPluginTest {

    companion object {
        // test cases properties file
        private val correct1PropertiesPath: String  = resource("dependencies_correct1.properties")
        private val correct2PropertiesPath: String  = resource("dependencies_correct2.properties")
        private val wrong1PropertiesPath: String    = resource("dependencies_wrong1.properties")
        private val wrong2PropertiesPath: String    = resource("dependencies_wrong2.properties")
        private val wrong3PropertiesPath: String    = resource("dependencies_wrong3.properties")
        private val wrong4PropertiesPath: String    = resource("dependencies_wrong4.properties")
        private val wrong5PropertiesPath: String    = resource("dependencies_wrong5.properties")

        // test cases properties
        private val correct1Properties = Properties()
        private val correct2Properties = Properties()


        /** internally used simplified resource loader */
        private fun resource(path: String): String {
            return this::class.java.classLoader.getResource(path)!!.path.replace("%20", " ")
        }


        /** 0) Configuration to read properties once before running multiple tests using them */
        @Throws(IOException::class)
        @BeforeClass @JvmStatic fun configureTestsuite() {
            correct1Properties.load(FileInputStream(correct1PropertiesPath))
            correct2Properties.load(FileInputStream(correct2PropertiesPath))
        }
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

        Assert.assertEquals(false, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
    }


    /** 2) Tests only applying the plugin (with one environment variable used for configuration) */
    @Test fun testApplyPluginWithEnvironmentVariableToProject() {
        var project = ProjectBuilder.builder().build()

        withEnvironmentVariable(
            "plugins.uniformdependencies.path", correct1PropertiesPath
        ).execute {
            // assert the environment variable is set correctly
            Assert.assertEquals(correct1PropertiesPath, System.getenv("plugins.uniformdependencies.path"))

            // apply plugin
            project.pluginManager.apply(UniformDependenciesPlugin::class.java)

            // assert that plugin is loaded
            Assert.assertEquals(true, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }

        project = ProjectBuilder.builder().build()

        withEnvironmentVariable(
            "plugins.uniformdependencies.path", correct2PropertiesPath
        ).execute {
            // assert the environment variable is set correctly
            Assert.assertEquals(correct2PropertiesPath, System.getenv("plugins.uniformdependencies.path"))

            // apply plugin
            project.pluginManager.apply(UniformDependenciesPlugin::class.java)

            // assert that plugin is loaded
            Assert.assertEquals(true, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }
    }


    /** 3) Tests only applying the plugin (with both environment variables used for configuration) */
    @Test fun testApplyPluginWithEnvironmentVariablesToProject() {
        val project = ProjectBuilder.builder().build()

        withEnvironmentVariable(
            "plugins.uniformdependencies.path", correct1PropertiesPath
        ).and(
            "plugins.uniformdependencies.strictness", Strictness.STRICT.toString()
        ).execute {
            // assert the environment variable is set correctly
            Assert.assertEquals(correct1PropertiesPath, System.getenv("plugins.uniformdependencies.path"))

            // apply plugin
            project.pluginManager.apply(UniformDependenciesPlugin::class.java)

            // assert that plugin is loaded
            Assert.assertEquals(true, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }
    }


    /** 4) Tests only applying the plugin (with both environment variables wrong used for configuration) */
    @Test fun testApplyPluginWithWrongEnvironmentVariablesToProject() {
        var project = ProjectBuilder.builder().build()

        withEnvironmentVariable(
            "plugins.uniformdependencies.path", correct1PropertiesPath
        ).and(
            "plugins.uniformdependencies.strictness", "Banana"
        ).execute {
            try {
                // try applying plugin (should fail)
                project.pluginManager.apply(UniformDependenciesPlugin::class.java)
            } catch (e: Exception) {
                // assert applying did not work
                assert(e.cause is WrongStrictnessLevelException)
            }

            Assert.assertEquals(false, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }

        project = ProjectBuilder.builder().build()

        withEnvironmentVariable(
            "plugins.uniformdependencies.path", "Banana"
        ).and(
            "plugins.uniformdependencies.strictness", Strictness.LOOSELY.toString()
        ).execute {
            try {
                // try applying plugin (should fail)
                project.pluginManager.apply(UniformDependenciesPlugin::class.java)
            } catch (e: Exception) {
                // assert applying did not work
                assert(e.cause is WrongDependenciesPathException)
            }

            Assert.assertEquals(false, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }
    }


    /** 5) Tests only applying the plugin (with both environment variables used for configuration) */
    @Test fun testApplyPluginWithEnvironmentVariablesWrongDataToProject() {
        listOf(
            wrong1PropertiesPath, wrong2PropertiesPath, wrong3PropertiesPath, wrong4PropertiesPath, wrong5PropertiesPath
        ).forEach {
            val project = ProjectBuilder.builder().build()
            withEnvironmentVariable(
                "plugins.uniformdependencies.path", it
            ).execute {
                // assert the environment variable is set correctly
                Assert.assertEquals(it, System.getenv("plugins.uniformdependencies.path"))

                try {
                    // try applying plugin (should fail)
                    project.pluginManager.apply(UniformDependenciesPlugin::class.java)
                } catch (e: Exception) {
                    // assert applying did not work
                    assert(e.cause is ParsingDependenciesException)
                }

                Assert.assertEquals(false, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
            }
        }
    }


    /** 6) Tests only applying the plugin (with project gradle.properties used for configuration) */
    @Test fun testApplyPluginWithGradlePropertiesToProject() {
        var project = ProjectBuilder.builder().build()
        with(project) {
            // project gradle.properties reference (project.properties.set can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            propertiesExtension["plugins.uniformdependencies.path"] = correct1PropertiesPath
            propertiesExtension["plugins.uniformdependencies.strictness"] = Strictness.STRICT.toString()

            // apply plugin
            project.pluginManager.apply(UniformDependenciesPlugin::class.java)

            // assert that plugin is loaded
            Assert.assertEquals(true, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }

        project = ProjectBuilder.builder().build()
        with(project) {
            // project gradle.properties reference (project.properties.set can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            propertiesExtension["plugins.uniformdependencies.path"] = correct1PropertiesPath
            propertiesExtension["plugins.uniformdependencies.strictness"] = Strictness.LOOSELY.toString()

            // apply plugin
            project.pluginManager.apply(UniformDependenciesPlugin::class.java)

            // assert that plugin is loaded
            Assert.assertEquals(true, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }

        project = ProjectBuilder.builder().build()
        with(project) {
            // project gradle.properties reference (project.properties.set can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            propertiesExtension["plugins.uniformdependencies.path"] = correct1PropertiesPath
            propertiesExtension["plugins.uniformdependencies.strictness"] = Strictness.LOOSE.toString()

            // apply plugin
            project.pluginManager.apply(UniformDependenciesPlugin::class.java)

            // assert that plugin is loaded
            Assert.assertEquals(true, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }
    }


    /** 7) Tests only applying the plugin (with project gradle.properties wrong used for configuration) */
    @Test fun testApplyPluginWithWrongGradlePropertiesToProject() {
        var project = ProjectBuilder.builder().build()
        with(project) {
            // project gradle.properties reference (project.properties.set can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            propertiesExtension["plugins.uniformdependencies.path"] = correct1PropertiesPath
            propertiesExtension["plugins.uniformdependencies.strictness"] = "Banana"

            try {
                // try applying plugin (should fail)
                project.pluginManager.apply(UniformDependenciesPlugin::class.java)
            } catch (e: Exception) {
                // assert applying did not work
                assert(e.cause is WrongStrictnessLevelException)
            }

            Assert.assertEquals(false, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }

        project = ProjectBuilder.builder().build()
        with(project) {
            // project gradle.properties reference (project.properties.set can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            propertiesExtension["plugins.uniformdependencies.path"] = correct1PropertiesPath
            propertiesExtension["plugins.uniformdependencies.strictness"] = "Banana"

            try {
                // try applying plugin (should fail)
                project.pluginManager.apply(UniformDependenciesPlugin::class.java)
            } catch (e: Exception) {
                // assert applying did not work
                assert(e.cause is WrongStrictnessLevelException)
            }

            Assert.assertEquals(false, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }

        project = ProjectBuilder.builder().build()
        with(project) {
            // project gradle.properties reference (project.properties.set can not be used directly!)
            val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            propertiesExtension["plugins.uniformdependencies.path"] = correct1PropertiesPath
            propertiesExtension["plugins.uniformdependencies.strictness"] = "Banana"

            try {
                // try applying plugin (should fail)
                project.pluginManager.apply(UniformDependenciesPlugin::class.java)
            } catch (e: Exception) {
                // assert applying did not work
                assert(e.cause is WrongStrictnessLevelException)
            }

            Assert.assertEquals(false, project.plugins.hasPlugin(UniformDependenciesPlugin::class.java))
        }
    }


    /** 8) Tests applying the plugin and evaluates that the extension set by plugin exists */
    @Test fun testEvaluatePluginExtension() {
        val project = ProjectBuilder.builder().build()

        // project gradle.properties reference (project.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        propertiesExtension["plugins.uniformdependencies.path"] = correct1PropertiesPath

        // apply plugin
        project.pluginManager.apply(UniformDependenciesPlugin::class.java)

        // assert that extension exists and is configured correctly
        val extension = project.extensions.getByType(UniformDependenciesPluginExtension::class.java)

        Assert.assertEquals(correct1PropertiesPath, extension.path.get())
        Assert.assertEquals(Strictness.LOOSELY, extension.strictness.get())

        val dependenciesFromFile = UniformDependenciesPlugin.parseDependenciesList(
            File(correct1PropertiesPath).absolutePath
        ).toList()
        val dependenciesExtension = extension.dependencies.get().split(";")

        Assert.assertEquals(dependenciesFromFile.size, dependenciesExtension.size)
        dependenciesFromFile.forEach {
            Assert.assertEquals(true, dependenciesExtension.contains("${it.group}:${it.name}:${it.version}"))
        }
    }


    /** 9) Tests applying the plugin and evaluates that Java plugin was applied */
    @Test fun testEvaluateCorrectnessJavaPlugin() {
        val project = ProjectBuilder.builder().build()

        // project gradle.properties reference (project.properties.set can not be used directly!)
        val propertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)
        propertiesExtension["plugins.uniformdependencies.path"] = correct1PropertiesPath

        // apply plugin
        project.pluginManager.apply(UniformDependenciesPlugin::class.java)

        // assert that Java plugin was applied to the project
        Assert.assertEquals(true, project.plugins.hasPlugin(JavaPlugin::class.java))
    }
}
