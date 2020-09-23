package org.unbrokendome.gradle.plugins.xjc.work.common

import com.sun.tools.xjc.ErrorReceiver
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.xml.sax.SAXParseException


internal class LoggingErrorReceiver : ErrorReceiver() {

    companion object {
        private val logger: Logger = Logging.getLogger(LoggingErrorReceiver::class.java)
    }


    override fun info(exception: SAXParseException) {
        if (logger.isInfoEnabled) {
            logger.info("{}\n{}", exception.message, getLocationString(exception))
        }
    }


    override fun warning(exception: SAXParseException) {
        if (logger.isWarnEnabled) {
            logger.warn("{}\n{}", exception.message, getLocationString(exception), exception)
        }
    }


    override fun error(exception: SAXParseException) {
        if (logger.isErrorEnabled) {
            logger.error("{}\n{}", exception.message, getLocationString(exception), exception)
        }
    }


    override fun fatalError(exception: SAXParseException) =
        error(exception)
}
