package org.unbrokendome.gradle.plugins.xjc

import com.sun.tools.xjc.ErrorReceiver
import org.gradle.api.logging.Logger
import org.xml.sax.SAXParseException


class LoggingErrorReporter extends ErrorReceiver {

    private final Logger logger

    LoggingErrorReporter(Logger logger) {
        this.logger = logger
    }


    @Override
    void fatalError(SAXParseException exception) {
        error(exception)
    }


    @Override
    void error(SAXParseException exception) {
        logger.error '{}\n{}', exception.message, getLocationString(exception)
    }


    @Override
    void warning(SAXParseException exception) {
        logger.warn '{}\n{}', exception.message, getLocationString(exception)
    }

    @Override
    void info(SAXParseException exception) {
        logger.info '{}\n{}', exception.message, getLocationString(exception)
    }
}
