package org.unbrokendome.gradle.plugins.xjc.work.common

import com.sun.tools.xjc.Options
import java.io.Closeable
import java.io.IOException
import java.net.URLClassLoader

class ContextClassLoaderHolderV21 : IContextClassLoaderHolder {
    private var contextClassLoader: ClassLoader? = null

    override fun setup(options: Options) {
        // Set up the classloader containing the plugin classpath. This should happen before any call
        // to parseArgument() or parseArguments() because it might trigger the resolution of plugins
        // (which are then cached)
        contextClassLoader = Thread.currentThread().contextClassLoader
        var userClassLoader = contextClassLoader
        if(!options.classpaths.isEmpty())
            userClassLoader = URLClassLoader(options.classpaths.toTypedArray(), contextClassLoader);
        //var userClassLoader = options.getUserClassLoader(contextClassLoader)
        Thread.currentThread().contextClassLoader = userClassLoader
    }

    override fun restore() {
        if(contextClassLoader != null) {
            val userClassLoader = Thread.currentThread().contextClassLoader
            Thread.currentThread().contextClassLoader = contextClassLoader
            if(userClassLoader != contextClassLoader && userClassLoader is Closeable) {
                try {
                    userClassLoader.close()
                } catch(_: IOException) {
                }
            }
            contextClassLoader = null
        }
    }

}
