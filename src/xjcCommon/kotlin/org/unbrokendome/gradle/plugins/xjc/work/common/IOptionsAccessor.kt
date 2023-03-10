package org.unbrokendome.gradle.plugins.xjc.work.common

interface IOptionsAccessor {

    fun hasEncoding(): Boolean

    fun getEncoding(): String?

    fun setEncoding(encoding: String?): Unit

}
