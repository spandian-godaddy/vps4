package com.godaddy.vps4.web.vm;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
    public void testConfigGetInProvideCommandService() throws Exception {
        Config mockConfig = Mockito.mock(Config.class);
        String baseUrl = UUID.randomUUID().toString();
        when(mockConfig.get("orchestration.url")).thenReturn(baseUrl);
        when(mockConfig.get("orchestration.api.certPath")).thenReturn("mock-cert");
        when(mockConfig.get("orchestration.api.keyPath")).thenReturn("mock-key");
        when(mockConfig.getData("mock-cert")).thenReturn("cert".getBytes(StandardCharsets.UTF_8));
        when(mockConfig.getData("mock-key")).thenReturn("key".getBytes(StandardCharsets.UTF_8));

        CommandClientModule cmdClientModule = new CommandClientModule();
        Method method = cmdClientModule.getClass().getDeclaredMethod("provideCommandService", Config.class);
        method.setAccessible(true);
        method.invoke(cmdClientModule, mockConfig);

        verify(mockConfig, Mockito.times(1)).get("orchestration.url");
    }
}
