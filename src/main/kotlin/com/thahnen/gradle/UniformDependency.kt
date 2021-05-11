package com.thahnen.gradle


/**
 *  UniformDependency:
 *  =================
 *
 *  internally used data class for storing dependencies parsed from properties file provided
 *
 *  @author Tobias Hahnen
 */
data class UniformDependency(var group: String, var name: String, var version: String)
