package com.godaddy.vps4.web;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.swagger.SwaggerModule;
import com.godaddy.hfs.web.CorsFilter;
import com.godaddy.hfs.web.GuiceFilterModule;
import com.godaddy.vps4.cache.HazelcastCacheModule;
import com.godaddy.vps4.cpanel.CpanelModule;
import com.godaddy.vps4.hfs.HfsClientModule;
import com.godaddy.vps4.hfs.HfsMockModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.sso.SsoModule;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.network.NetworkModule;
import com.godaddy.vps4.web.security.AuthenticationFilter;
import com.godaddy.vps4.web.security.Vps4UserFakeModule;
import com.godaddy.vps4.web.security.Vps4UserModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.ServletModule;

public class Vps4Injector {

    private static final Logger logger = LoggerFactory.getLogger(Vps4Injector.class);

    private static boolean useFakeUser = System.getProperty("vps4.user.fake", "false").equals("true");

    private static final Injector INJECTOR = newInstance();

    public Injector getInstance() {
        return INJECTOR;
    }

    static Injector newInstance() {
        List<Module> modules = new ArrayList<>();

        modules.add(new ListenerModule());
        modules.add(new GuiceFilterModule());
        modules.add(new SwaggerModule());

        if (System.getProperty("vps4.hfs.mock", "false").equals("true")) {
            modules.add(new HfsMockModule());
            logger.info("USING MOCK HFS");
        }
        else{
            modules.add(new HfsClientModule());
        }


        modules.add(getUserModule(useFakeUser));

        modules.add(new DatabaseModule());
        modules.add(new WebModule());
        modules.add(new SecurityModule());
        modules.add(new SsoModule());

        modules.add(new VmModule());
        modules.add(new NetworkModule());
        //modules.add(new FakeCpanelModule());
        modules.add(new CpanelModule());
        modules.add(new CommandClientModule());
        modules.add(new ServletModule() {
            @Override
            public void configureServlets() {

                bind(CorsFilter.class).in(Singleton.class);
                filter("/api/*").through(CorsFilter.class);

                if (!useFakeUser) {
                    bind(AuthenticationFilter.class).in(Singleton.class);
                    filter("/api/*").through(AuthenticationFilter.class);
                }
            }
        });
        modules.add(new HazelcastCacheModule());

        return Guice.createInjector(modules);
    }

    private static Module getUserModule(boolean useFakeUser) {
        if (useFakeUser) {
            logger.info("USING FAKE USER");
            return new Vps4UserFakeModule();
        }
        else {
            return new Vps4UserModule();
        }
    }
}
