package com.godaddy.hfs.web.resteasy;

import java.util.function.Function;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.google.inject.Injector;

public class GuiceResteasyBootstrap extends ResteasyBootstrap {

    @Inject private Injector injector = null;

    final Function<Class, Boolean> allowFilter;

    public GuiceResteasyBootstrap(Function<Class, Boolean> allowFilter) {
        this.allowFilter = allowFilter;
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
       super.contextInitialized(event);
       final ServletContext context = event.getServletContext();
       final Registry registry = (Registry) context.getAttribute(Registry.class.getName());
       final ResteasyProviderFactory providerFactory = (ResteasyProviderFactory) context.getAttribute(ResteasyProviderFactory.class.getName());
       final HfsModuleProcessor processor = new HfsModuleProcessor(registry, providerFactory, allowFilter);

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
