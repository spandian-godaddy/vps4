package com.godaddy.vps4.orchestration.network;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import com.godaddy.vps4.ipblacklist.IpBlacklistService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class RemoveIpFromBlacklistTest {

    IpBlacklistService ipBlacklistService = mock(IpBlacklistService.class);
    RemoveIpFromBlacklist command = new RemoveIpFromBlacklist(ipBlacklistService);

    Injector injector = Guice.createInjector(binder -> { });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Test
    public void executeRemoveIpFromBlacklistTest() {
        command.execute(context, "192.168.0.1");
        verify(ipBlacklistService).removeIpFromBlacklist("192.168.0.1");
    }

}
