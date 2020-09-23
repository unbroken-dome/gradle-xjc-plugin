package org.unbrokendome.gradle.plugins.xjc.work.common

import com.sun.tools.xjc.ErrorReceiver
import com.sun.tools.xjc.XJCListener
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.xml.sax.SAXParseException


class LoggingXjcListener : XJCListener() {

    companion object {
        private val logger: Logger = Logging.getLogger(LoggingXjcListener::class.java)
    }


    private val errorReceiver: ErrorReceiver = LoggingErrorReceiver()


    override fun generatedFile(fileName: String, current: Int, total: Int) {
        logger.info("Generated: {} ({} of {})", fileName, current, total)
    }


    override fun message(msg: String) {
        logger.info(msg)
    }


    override fun info(exception: SAXParseException) {
        errorReceiver.info(exception)
    }


    override fun warning(exception: SAXParseException) {
        errorReceiver.warning(exception)
    }


    override fun error(exception: SAXParseException) {
        errorReceiver.error(exception)
    }


    override fun fatalError(exception: SAXParseException) {
        errorReceiver.fatalError(exception)
    }
}
