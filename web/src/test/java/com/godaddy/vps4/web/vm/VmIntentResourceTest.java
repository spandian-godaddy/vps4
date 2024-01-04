package com.godaddy.vps4.web.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.intent.IntentService;
import com.godaddy.vps4.intent.model.Intent;
import com.godaddy.vps4.web.Vps4Exception;

public class VmIntentResourceTest {
    
    public VmIntentResource vmIntentResource;
    private IntentService intentService;
    private VmResource vmResource;

    @Before
    public void setUp() {
        List<Intent> intents = new ArrayList<>();
        intents.add(new Intent(1, "intent1", "description1"));
        intents.add(new Intent(2, "intent2", "description2"));
        intents.add(new Intent(3, "intent3", "description3"));
        intentService = mock(IntentService.class);
        when(intentService.getIntents()).thenReturn(intents);
        when(intentService.getVmIntents(any())).thenReturn(intents);
        when(intentService.setVmIntents(any(), any())).thenReturn(intents);

        vmResource = mock(VmResource.class);
        vmIntentResource = new VmIntentResource(intentService, vmResource);
    }

    @Test
    public void testGetVmIntentOptions() {
        List<Intent> intents = vmIntentResource.getVmIntentOptions();
        assert(intents.size() == 3);
    }

    @Test
    public void testGetVmIntents() {
        List<Intent> intents = vmIntentResource.getVmIntents(UUID.randomUUID());
        assert(intents.size() == 3);
    }

    @Test
    public void testSetVmIntents() {
        UUID vmId = UUID.randomUUID();
        List<Integer> intentIds = new ArrayList<>();
        intentIds.add(1);
        intentIds.add(2);
        intentIds.add(3);
        List<Intent> intents = vmIntentResource.setVmIntents(vmId, intentIds, "otherIntentDescription");
        assert(intents.size() == 3);
        verify(intentService, times(1)).setVmIntents(eq(vmId), any());
    }

    @Test (expected = Vps4Exception.class)
    public void testSetVmIntentsInvalidId() {
        UUID vmId = UUID.randomUUID();
        List<Integer> intentIds = new ArrayList<>();
        intentIds.add(4);
        List<Intent> intents = vmIntentResource.setVmIntents(vmId, intentIds, "otherIntentDescription");
        assert(intents.size() == 3);
        verify(intentService, times(1)).setVmIntents(eq(vmId), any());
    }
}


