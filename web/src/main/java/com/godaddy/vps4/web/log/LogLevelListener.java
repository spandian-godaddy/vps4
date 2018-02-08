package com.godaddy.vps4.web.log;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import ch.qos.logback.classic.Level;
import com.godaddy.hfs.config.Config;
import com.google.inject.Inject;

public class LogLevelListener implements ServletContextListener {

    Config config;

    @Inject
    public LogLevelListener(Config config) {
        this.config = config;
    }

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        Level level = Level.toLevel(config.get("vps4.log.level.web"), Level.INFO);
        root.setLevel(level);

        root.warn("Log level set to {}", level.levelStr);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		
	}
}