package org.unbrokendome.gradle.plugins.xjc

import com.sun.tools.xjc.ErrorReceiver
import com.sun.tools.xjc.XJCListener
import org.gradle.api.logging.Logger
import org.xml.sax.SAXParseException


class LoggingXjcListener extends XJCListener {

    private final Logger logger
    private final ErrorReceiver errorReceiver


    LoggingXjcListener(Logger logger) {
        this.logger = logger
        this.errorReceiver = new LoggingErrorReporter(logger)
    }


    @Override
    void generatedFile(String fileName, int current, int total) {
        logger.info 'Generated: {} ({} of {})', fileName, current, total
    }


    @Override
    void message(String msg) {
        logger.info msg
    }


    @Override
    void fatalError(SAXParseException exception) {
        errorReceiver.fatalError exception
    }


    @Override
    void error(SAXParseException exception) {
        errorReceiver.error exception
    }


    @Override
    void warning(SAXParseException exception) {
        errorReceiver.warning exception
    }


    @Override
    void info(SAXParseException exception) {
        errorReceiver.info exception
    }
}
