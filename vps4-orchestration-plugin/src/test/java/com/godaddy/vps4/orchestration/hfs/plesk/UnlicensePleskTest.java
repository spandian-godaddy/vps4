package com.godaddy.vps4.orchestration.hfs.plesk;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;

public class UnlicensePleskTest {

    PleskService pleskService = mock(PleskService.class);
    WaitForPleskAction waitAction = mock(WaitForPleskAction.class);
    PleskAction pleskAction = mock(PleskAction.class);
    Long hfsVmId = 42L;

    UnlicensePlesk command = new UnlicensePlesk(pleskService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForPleskAction.class).toInstance(waitAction);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Before
    public void setUp() {
        when(pleskService.licenseRelease(hfsVmId)).thenReturn(pleskAction);
    }

    @Test
    public void executesPleskLicenseRelease() {
        command.execute(context, hfsVmId);
        verify(pleskService).licenseRelease(hfsVmId);
    }

    @Test
    public void executesWaitForPleskAction() {
        command.execute(context, hfsVmId);
        verify(context).execute(WaitForPleskAction.class, pleskAction);
    }

    @Test
    public void ignoresNoVmResourceIdException() {
        String hfsResponse = "{\n" +
                "  \"id\": \"HFS:INTERNAL_ERROR\",\n" +
                "  \"stackTrace\": [\n" +
                "    \"gdg.hfs.vhfs.plesk.web.RestPleskService.serverToVM:400\",\n" +
                "    \"gdg.hfs.vhfs.plesk.web.RestPleskService.licenseReleaseNydus:299\",\n" +
                "    \"gdg.hfs.vhfs.plesk.web.RestPleskService.licenseRelease:292\",\n" +
                "  ],\n" +
                "  \"message\": \"VM does not have a resource ID associated with it\",\n" +
                "  \"status\": 422\n" +
                "}";
        Response response = Response.status(422).entity(hfsResponse).build();
        doThrow(new ClientErrorException(response))
            .when(pleskService).licenseRelease(hfsVmId);
        command.execute(context, hfsVmId);
    }

    @Test(expected=ClientErrorException.class)
    public void throwsUnexpectedErrorException() {
        Response response = Response.status(422).entity("Crazy unexpected unlicense exception").build();
        doThrow(new ClientErrorException(response))
            .when(context).execute(WaitForPleskAction.class, pleskAction);
        command.execute(context, hfsVmId);
    }

}
