package com.godaddy.vps4.web;

import java.net.URL;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwaggerContextHandler {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerContextHandler.class);

    private static final String RESOURCE_PATH = "/com/godaddy/swagger/assets";

    private static final String DEFAULT_CONTEXT_PATH = "/swagger";

    public static ContextHandler newSwaggerResourceContext() {
        return newSwaggerResourceContext(DEFAULT_CONTEXT_PATH);
    }

    public static ContextHandler newSwaggerResourceContext(String contextPath) {

        URL baseResource = SwaggerContextHandler.class.getResource(RESOURCE_PATH);
        if (baseResource == null) {
            throw new IllegalStateException("Web home not found for Orchestration Web API at: " + RESOURCE_PATH);
        }

        logger.info("swagger resource context at {}", contextPath);

        // resource context
        ContextHandler resourceContext = new ContextHandler();
        resourceContext.setContextPath(contextPath);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setBaseResource(Resource.newClassPathResource(RESOURCE_PATH, true, true));
        resourceContext.setHandler(resourceHandler);

        return resourceContext;
    }

}
