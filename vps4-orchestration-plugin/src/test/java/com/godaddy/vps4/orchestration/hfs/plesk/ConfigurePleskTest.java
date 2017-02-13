/**
 * Unit test for ConfigurePlesk
 */
package com.godaddy.vps4.orchestration.hfs.plesk;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskAction.Status;
import gdg.hfs.vhfs.plesk.PleskService;

/**
 * @author abhoite
 *
 */

public class ConfigurePleskTest {

    PleskService pleskService;
    ConfigurePlesk command;
    CommandContext context;
    Injector injector;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setup() throws Exception {

        pleskService = mock(PleskService.class);
        command = new ConfigurePlesk(pleskService);
        injector = Guice.createInjector(binder -> {
            binder.bind(ConfigurePlesk.class);
            binder.bind(WaitForPleskAction.class);
            binder.bind(PleskService.class).toInstance(pleskService);
        });
        context = new TestCommandContext(new GuiceCommandProvider(injector));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        pleskService = null;
        command = null;
        injector = null;
        context = null;
    }

    /**
     * Test method for {@link com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk#execute(gdg.hfs.orchestration.CommandContext, com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest)}.
     */
    @Test
    public void testExecuteSuccess() {
        ConfigurePleskRequest request = new ConfigurePleskRequest(777L, "fake-user", "super-secret-password");

        PleskAction pleskAction = new PleskAction();
        pleskAction.actionId = 555;
        pleskAction.status = Status.COMPLETE;

        when(pleskService.imageConfig(request.vmId, request.username, request.password)).thenReturn(pleskAction);
        when(pleskService.getAction(pleskAction.actionId)).thenReturn(pleskAction);

        command.execute(context, request);

        verify(pleskService, times(1)).imageConfig(request.vmId, request.username, request.password);
    }

    @Test(expected = RuntimeException.class)
    public void failPleskImageConfig() throws Exception {
        ConfigurePleskRequest request = new ConfigurePleskRequest(777L, "fake-user", "super-secret-password");

        // if HFS throws an exception on pleskService, the command should fail
        when(pleskService.imageConfig(request.vmId, request.username, request.password)).thenThrow(new RuntimeException("Faked an HFS failure"));

        command.execute(context, request);
    }
}
