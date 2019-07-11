package com.godaddy.vps4.orchestration.hfs.dns;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.hfs.dns.HfsDnsAction;
import com.godaddy.hfs.dns.HfsDnsService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.dns.WaitForDnsAction;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class CreateDnsPtrRecordTest {
    HfsDnsService hfsDnsService = mock(HfsDnsService.class);
    WaitForDnsAction waitAction = mock(WaitForDnsAction.class);

    CreateDnsPtrRecord.Request request = new CreateDnsPtrRecord.Request();
    CreateDnsPtrRecord command = new CreateDnsPtrRecord(hfsDnsService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForDnsAction.class).toInstance(waitAction);
    });

    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Test
    public void testExecuteSuccess() {
        VirtualMachine vm = mock(VirtualMachine.class);
        request.virtualMachine = vm;
        request.reverseDnsName = "fake.dns.name";
        HfsDnsAction hfsDnsAction = mock(HfsDnsAction.class);

        when(hfsDnsService.createDnsPtrRecord(request.virtualMachine.hfsVmId, request.reverseDnsName)).thenReturn(hfsDnsAction);
        command.execute(context, request);
        verify(hfsDnsService, times(1)).createDnsPtrRecord(vm.hfsVmId, request.reverseDnsName);
        verify(context, times(1)).execute(WaitForDnsAction.class, hfsDnsAction);
    }

}
