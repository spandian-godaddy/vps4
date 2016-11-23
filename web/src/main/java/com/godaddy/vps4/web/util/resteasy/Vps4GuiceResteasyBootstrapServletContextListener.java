package com.godaddy.vps4.web.util.resteasy;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.google.inject.Injector;

public class Vps4GuiceResteasyBootstrapServletContextListener extends ResteasyBootstrap implements ServletContextListener {

    @Inject private Injector injector = null;

    @Override
    public void contextInitialized(final ServletContextEvent event)
    {
       super.contextInitialized(event);
       final ServletContext context = event.getServletContext();
       final Registry registry = (Registry) context.getAttribute(Registry.class.getName());
       final ResteasyProviderFactory providerFactory = (ResteasyProviderFactory) context.getAttribute(ResteasyProviderFactory.class.getName());
       final Vps4ModuleProcessor processor = new Vps4ModuleProcessor(registry, providerFactory);

       Injector injector = this.injector;

       if (injector == null) {
           throw new IllegalStateException("No injector found");
       }
       processor.processInjector(injector);

       //load parent injectors
       while (injector.getParent() != null) {
          injector = injector.getParent();
          processor.processInjector(injector);
       }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event)
    {

    }

}
