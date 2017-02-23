package com.godaddy.vps4.web;

import javax.servlet.ServletContextListener;

import com.godaddy.hfs.web.resteasy.HfsGuiceResteasyBootstrapServletContextListener;
import com.godaddy.vps4.cache.CacheLifecycleListener;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class ListenerModule extends AbstractModule {

    @Override
    public void configure() {
        Multibinder<ServletContextListener> mb = Multibinder.newSetBinder(binder(), ServletContextListener.class);

        mb.addBinding().to(CacheLifecycleListener.class);

        mb.addBinding().toInstance(new HfsGuiceResteasyBootstrapServletContextListener(
                beanClass -> beanClass.isAnnotationPresent(Vps4Api.class)
                            || beanClass.getName().startsWith("io.swagger")
        ));
    }
}
