package com.godaddy.vps4.web.vm;

import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.web.CommandClientModule;

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
            Method method = cmdClientModule.getClass().getDeclaredMethod("provideCommandService", Config.class);
            method.setAccessible(true);
            method.invoke(cmdClientModule, mockConfig);
        }
        catch (Exception ex) {
            Assert.fail(ex.toString());
        }

        Mockito.verify(mockConfig, Mockito.times(1)).get("orchestration.url");
    }
}
