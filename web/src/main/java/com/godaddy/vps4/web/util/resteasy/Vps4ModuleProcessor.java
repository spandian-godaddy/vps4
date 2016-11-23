package com.godaddy.vps4.web.util.resteasy;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.guice.GuiceResourceFactory;
import org.jboss.resteasy.plugins.guice.i18n.LogMessages;
import org.jboss.resteasy.plugins.guice.i18n.Messages;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.GetRestful;

import com.godaddy.vps4.web.Vps4Api;
import com.google.inject.Binding;
import com.google.inject.Injector;

public class Vps4ModuleProcessor {

    private final Registry registry;
    private final ResteasyProviderFactory providerFactory;

    public Vps4ModuleProcessor(final Registry registry, final ResteasyProviderFactory providerFactory) {
        this.registry = registry;
        this.providerFactory = providerFactory;
    }

    public void processInjector(final Injector injector) {
        List<Binding<?>> rootResourceBindings = new ArrayList<>();
        for (final Binding<?> binding : injector.getBindings().values()) {
            final Type type = binding.getKey().getTypeLiteral().getRawType();
            if (type instanceof Class) {
                final Class<?> beanClass = (Class) type;
                if (isResourceClass(beanClass)) {
                    // deferred registration
                    rootResourceBindings.add(binding);
                }
                if (beanClass.isAnnotationPresent(Provider.class)) {
                    LogMessages.LOGGER.info(Messages.MESSAGES.registeringProviderInstance(beanClass.getName()));
                    providerFactory.registerProviderInstance(binding.getProvider().get());
                }
            }
        }
        for (Binding<?> binding : rootResourceBindings) {
            Class<?> beanClass = (Class) binding.getKey().getTypeLiteral().getType();
            final ResourceFactory resourceFactory = new GuiceResourceFactory(binding.getProvider(), beanClass);
            LogMessages.LOGGER.info(Messages.MESSAGES.registeringFactory(beanClass.getName()));
            registry.addResourceFactory(resourceFactory);
        }
    }

    static boolean isResourceClass(Class beanClass) {
        return GetRestful.isRootResource(beanClass)
                && (beanClass.isAnnotationPresent(Vps4Api.class)
                        || beanClass.getName().startsWith("io.swagger"));

    }
}
