package com.godaddy.vps4.web.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.UUID;

import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandService;

public class DateConverterTest {

    @Test
    public void testSupportDate() throws ParseException, URISyntaxException{
        // A bug was shipped that had 2017-03-20 01:02:03 msc as an example time.  msc is not a valid time zone.
        ActionService actionService = Mockito.mock(ActionService.class);
        CommandService commandService = Mockito.mock(CommandService.class);
        Config config = Mockito.mock(Config.class);
        when(config.get(Mockito.anyString())).thenReturn("0");
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getAbsolutePath()).thenReturn(new URI("http://127.0.0.1/"));
        UUID vmId = UUID.randomUUID();
        SupportResource support = new SupportResource(actionService, null, null, null, commandService, config);

        support.getActions(vmId, 11, 1, null, "2017-01-01 01:02:03 GMT", "2017-03-20 01:02:03 GMT", true, uriInfo);
        support.getActions(vmId, 11, 1, null, null, null, true, uriInfo);

        //No Exceptions thrown means that the example date format is valid.
    }

}
