package org.unbrokendome.gradle.plugins.xjc.work.common

import com.sun.tools.xjc.Options

interface IContextClassLoaderHolder {

    fun restore()

    fun setup(options: Options)

}
