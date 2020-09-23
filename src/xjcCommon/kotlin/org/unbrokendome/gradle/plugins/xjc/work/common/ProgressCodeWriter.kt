package org.unbrokendome.gradle.plugins.xjc.work.common

import com.sun.codemodel.CodeWriter
import com.sun.codemodel.JPackage
import com.sun.codemodel.writer.FilterCodeWriter
import com.sun.tools.xjc.AbortException
import com.sun.tools.xjc.XJCListener
import java.io.File
import java.io.OutputStream
import java.io.Writer


internal class ProgressCodeWriter(
    core: CodeWriter,
    private val progress: XJCListener,
    private val totalFileCount: Int
) : FilterCodeWriter(core) {

    private var current = 0


    override fun openSource(pkg: JPackage?, fileName: String): Writer {
        report(pkg, fileName)
        return super.openSource(pkg, fileName)
    }


    override fun openBinary(pkg: JPackage?, fileName: String): OutputStream {
        report(pkg, fileName)
        return super.openBinary(pkg, fileName)
    }


    private fun report(pkg: JPackage?, fileName: String) {
        val targetPath = if (pkg != null && pkg.name().isNotEmpty()) {
            val packagePath = pkg.name().replace('.', File.separatorChar)
            "$packagePath${File.separatorChar}${fileName}"
        } else {
            fileName
        }

        if (progress.isCanceled) throw AbortException()

        progress.generatedFile(targetPath, ++current, totalFileCount)
    }

}
