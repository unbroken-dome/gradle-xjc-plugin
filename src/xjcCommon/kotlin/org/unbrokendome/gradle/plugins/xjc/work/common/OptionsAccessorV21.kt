package org.unbrokendome.gradle.plugins.xjc.work.common

import com.sun.tools.xjc.Options

// XJC Options v2.1
class OptionsAccessorV21(options: Options) : IOptionsAccessor {

    private val options = options

    override fun hasEncoding() = false

    override fun getEncoding(): String? = null

    override fun setEncoding(encoding: String?) {
        // NOOP
    }

}
