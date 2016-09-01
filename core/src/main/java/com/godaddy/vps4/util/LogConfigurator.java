package com.godaddy.vps4.util;

import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class LogConfigurator extends BasicConfigurator {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    public LogConfigurator() {

    }

    @Override
    public void configure(LoggerContext lc) {

        super.configure(lc);

        Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);

        Logger vps4Logger = lc.getLogger("com.godaddy.vps4");
        vps4Logger.setLevel(Level.DEBUG);

        // TODO
        // for production configuration, configure rotating log files

    }

}
