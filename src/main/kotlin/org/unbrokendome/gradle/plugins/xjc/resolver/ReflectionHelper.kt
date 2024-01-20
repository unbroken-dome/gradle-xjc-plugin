package org.unbrokendome.gradle.plugins.xjc.resolver

import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.regex.Pattern

class ReflectionHelper {

    // Trying to keep this ugliness away from the main code
    companion object {
        private val logger = LoggerFactory.getLogger(ExtensibleCatalogResolver::class.java)

        private val inputStreams: ConcurrentLinkedQueue<InputStream> = ConcurrentLinkedQueue()

        fun addInputStream(inputStream: InputStream) {
            inputStreams.add(inputStream)
        }

        fun closeAll() {
            if(inputStreams.size > 0)
                logger.debug("{}#closeAll() count={}", ReflectionHelper::class.java.simpleName, inputStreams.size)

            while (!inputStreams.isEmpty()) {
                val c = inputStreams.remove()

                try {
                    c.close()
                } catch(_: IOException) {
                }

                try {
                    if(c.javaClass.name.endsWith("JarURLInputStream")) {
                        // "sun.net.www.protocol.jar.JarURLConnection$JarURLInputStream"
                        reflectiveInvoke_jarFile_close(c);
                    }
                } catch(_: IOException) {
                }
            }
        }

        private fun reflectiveInvoke_jarFile_close(o: InputStream): Boolean? {
            // Ugly hacks for broken JRE features
            try {
                // Enclosing Class field reference to enclosing instance, name is JavaC convention
                val enclosingParentField = o.javaClass.getDeclaredField("this$0")
                enclosingParentField.isAccessible = true
                val enclosingParentInstance = enclosingParentField.get(o)   // JarURLConnection
                val klass = enclosingParentInstance.javaClass   // URLJarFile
                val jarFileField = klass.getDeclaredField("jarFile")
                jarFileField.isAccessible = true
                val jarFile = jarFileField.get(enclosingParentInstance)
                if(jarFile != null) {
                    val closeMethod = jarFile.javaClass.getMethod("close")
                    closeMethod.invoke(jarFile)
                    logger.debug("{}.jarFile.close()", o)
                    return true
                }
                return false
            } catch(_: NoSuchFieldException) {
            } catch(_: NoSuchMethodException) {
            } catch(_: IllegalArgumentException) {
            } catch(_: IllegalAccessException) {
            } catch(_: InvocationTargetException) {
            } catch(_: SecurityException) {
            } catch(e: Throwable) {
                // we catch this as the reflection approach is a best-effort and should not
                //   cause a terminal failure due to an obscure JVM being different here
                if(logger.isDebugEnabled)
                    logger.debug("{}", "", e)
                else if(isJre8OrEarlier())
                    logger.warn("{}: {}", e.javaClass.name, e.message)
            }
            return null
        }

        fun reflectiveInvoke_URLConnection_getDefaultUseCaches(protocol: String): Boolean? {
            // JDK8 compatibility
            try {
                val klass = Class.forName("java.net.URLConnection") // since 1.0
                val m = klass.getMethod("getDefaultUseCaches", String::class.java)  // JDK9+
                val rv = m.invoke(null, protocol) as Boolean
                logger.debug("URLConnection.getDefaultUseCaches({}) = {}", protocol, rv)
                return rv
            } catch(_: NoSuchMethodException) {
            } catch(_: InvocationTargetException) {
            } catch(_: SecurityException) {
            } catch(e: Throwable) {
                // we catch this as the reflection approach is a best-effort and should not
                //   cause a terminal failure due to an obscure JVM being different here
                if(logger.isDebugEnabled)
                    logger.debug("{}", "", e)
                else if(isJre8OrEarlier())
                    logger.warn("{}: {}", e.javaClass.name, e.message)
            }
            return null
        }

        fun reflectiveInvoke_URLConnection_setDefaultUseCaches(protocol: String, defaultVal: Boolean): Boolean {
            // JDK8 compatibility
            try {
                val klass = Class.forName("java.net.URLConnection") // since 1.0
                val m = klass.getMethod("setDefaultUseCaches", String::class.java, Boolean::class.java)  // JDK9+
                m.invoke(null, protocol, defaultVal)
                logger.debug("URLConnection.setDefaultUseCaches({}, {})", protocol, defaultVal)
                return true
            } catch(_: NoSuchMethodException) {
            } catch(_: InvocationTargetException) {
            } catch(_: SecurityException) {
            } catch(e: Throwable) {
                // we catch this as the reflection approach is a best-effort and should not
                //   cause a terminal failure due to an obscure JVM being different here
                if(logger.isDebugEnabled)
                    logger.debug("{}", "", e)
                else if(isJre8OrEarlier())
                    logger.warn("{}: {}", e.javaClass.name, e.message)
            }
            return false
        }

        private fun isJre8OrEarlier(): Boolean {
            val sysPropJavaVersion = System.getProperty("java.version")
            val pattern = Pattern.compile("^\\s*(\\d+).*")
            val versionMajor = pattern.matcher(sysPropJavaVersion).let {
                if(it.matches()) Integer.valueOf(it.group(1)) else 0
            }
            return versionMajor <= 8
        }
    }

}
