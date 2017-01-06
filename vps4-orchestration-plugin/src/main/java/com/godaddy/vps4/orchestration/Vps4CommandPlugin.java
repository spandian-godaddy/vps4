package com.godaddy.vps4.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.orchestration.hfs.HfsCommandModule;
import com.godaddy.vps4.orchestration.hfs.HfsMockModule;
import com.godaddy.vps4.orchestration.hfs.HfsModule;
import com.godaddy.vps4.vm.VmModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandPlugin;
import gdg.hfs.orchestration.CommandProvider;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class Vps4CommandPlugin implements CommandPlugin {
    
    private static final Logger logger = LoggerFactory.getLogger(Vps4CommandPlugin.class);

    @Override
    public String getName() {
        return "vps4";
    }

    @Override
    public CommandProvider newCommandProvider() {
        
        AbstractModule hfsModule = null;
        
        if (System.getProperty("vps4.hfs.mock", "false").equals("true")) {       
            hfsModule = new HfsMockModule();
            logger.info("USING MOCK HFS");        
         }     
         else{      
             hfsModule = new HfsModule();
         }
        
        Injector injector = Guice.createInjector(

                hfsModule,
                new HfsCommandModule(),

                new DatabaseModule(),
                new VmModule(),

                new Vps4CommandModule()
                );

        return new GuiceCommandProvider(injector);
    }

}
