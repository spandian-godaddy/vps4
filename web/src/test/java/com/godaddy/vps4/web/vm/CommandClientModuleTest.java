package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.web.CommandClientModule;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.hfs.config.Config;

import gdg.hfs.orchestration.client.HttpCommandService;

import java.lang.reflect.Method;
import java.util.UUID;

public class CommandClientModuleTest {

    @Test
    public void testConfigure() {
        CommandClientModule cmdClientModule = new CommandClientModule();
        cmdClientModule.configure();
    }

    @Test
    public void testConfigGetInProvideCommandService() {
        Config mockConfig = Mockito.mock(Config.class);
        String baseUrl = UUID.randomUUID().toString();
        Mockito.when(mockConfig.get("orchestration.url")).thenReturn(baseUrl);

        CommandClientModule cmdClientModule = new CommandClientModule();
        try {
            Method m = cmdClientModule.getClass().getDeclaredMethod("provideCommandService", Config.class);
            m.setAccessible(true);
            HttpCommandService commandService = (HttpCommandService)m.invoke(cmdClientModule.getClass(), mockConfig);
        }
        catch (Exception ex) {
            Assert.fail(ex.toString());
        }

        Mockito.verify(mockConfig, Mockito.times(1)).get("orchestration.url");
    }
}
