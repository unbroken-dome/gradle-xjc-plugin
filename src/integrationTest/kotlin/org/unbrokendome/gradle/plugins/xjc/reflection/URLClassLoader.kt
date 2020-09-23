package org.unbrokendome.gradle.plugins.xjc.reflection

import java.io.File
import java.net.URLClassLoader


fun URLClassLoader(vararg files: File): URLClassLoader =
    URLClassLoader(files.map { it.toURI().toURL() }.toTypedArray())
