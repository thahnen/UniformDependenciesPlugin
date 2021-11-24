package com.github.thahnen

import org.junit.Test

import kotlin.test.assertEquals


/**
 *  UniformDependenciesObjectTest:
 *  =============================
 *
 *  jUnit test cases on the UniformDependenciesObject
 */
class UniformDependenciesObjectTest {

    /** 1) Tests the UniformDependenciesObject for JaCoCo coverage */
    @Test fun testUniformDependenciesObject() {
        val dependency = UniformDependenciesObject("com.thahnen", "Test", "1.0.0")

        assertEquals("com.thahnen", dependency.group)
        assertEquals("Test", dependency.name)
        assertEquals("1.0.0", dependency.version)

        dependency.group    = "de.thahnen"
        dependency.name     = "Plugin"
        dependency.version  = "1.2.3"

        assertEquals("de.thahnen", dependency.group)
        assertEquals("Plugin", dependency.name)
        assertEquals("1.2.3", dependency.version)
    }
}
