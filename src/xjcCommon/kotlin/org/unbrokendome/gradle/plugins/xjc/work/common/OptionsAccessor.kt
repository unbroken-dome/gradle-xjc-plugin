package org.unbrokendome.gradle.plugins.xjc.work.common

import com.sun.tools.xjc.Options

// XJC Options v2.2+ through 4.0.x (the latest at this time)
class OptionsAccessor(options: Options) : IOptionsAccessor {

    private val options = options

    override fun hasEncoding() = true

    override fun getEncoding(): String? = options.encoding

    override fun setEncoding(encoding: String?) {
        options.encoding = encoding
    }

}
