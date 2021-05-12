package com.thahnen.gradle

import kotlin.IllegalArgumentException


/**
 *  Extension to String to create
 *
 *  @param index where to split the string
 *  @return pair consisting of substring up to index and substring behind index
 *  @throws IllegalArgumentException when index is out of bounds
 */
@Throws(IllegalArgumentException::class)
internal fun String.splitAtIndex(index: Int) : Pair<String, String> = require(index in 0..length).let {
    take(index) to substring(index)
}


/**
 *  Extension to Boolean to create a ternary operator
 */
internal infix fun <T: Any> Boolean.t(value: T) : T? = if (this) value else null
