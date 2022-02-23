package com.godaddy.vps4.web.customNotes;

import com.godaddy.vps4.customNotes.CustomNote;
import com.godaddy.vps4.customNotes.CustomNotesService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.Phase2ExternalsModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CustomNotesResourceTest {
    private VmResource vmResource = mock(VmResource.class);
    private CustomNotesService customNotesService = mock(CustomNotesService.class);

    private GDUser user;
    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();
    private Long hfsVmId = 42L;
    private VirtualMachine vps4Vm = new VirtualMachine();
    private Injector injector = Guice.createInjector(new DatabaseModule(),
            new SecurityModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(VmResource.class).toInstance(vmResource);
                    bind(CustomNotesService.class).toInstance(customNotesService);

                }
                @Provides
                public GDUser provideUser() {
                        return user;
                }
            });

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        injector.injectMembers(this);
        user = GDUserMock.createEmployee();
        vps4Vm.hfsVmId = hfsVmId;
        vps4Vm.orionGuid = orionGuid;
        vps4Vm.canceled = Instant.MAX;
        when(vmResource.getVm(vmId)).thenReturn(vps4Vm);
    }

    private CustomNotesResource getCustomNotesResource() {
        return injector.getInstance(CustomNotesResource.class);
    }

    @Test
    public void testGetCustomNoteCallsCustomNotesService() {
        getCustomNotesResource().getCustomNotes(vmId);

        verify(customNotesService,times(1)).getCustomNotes(eq(vmId));
    }

    @Test
    public void testCreateCustomNoteCallsGetVm() {
        getCustomNotesResource().createCustomNote(vmId, new CustomNotesResource.CustomNoteRequest());
        verify(vmResource,times(1)).getVm(eq(vmId));
    }

    @Test
    public void testCreateCustomNoteCallsCustomNotesServiceForAdmin() {
        String username = user.getUsername();
        CustomNotesResource.CustomNoteRequest request = new CustomNotesResource.CustomNoteRequest();
        request.note = "This is a test note created by admin";

        getCustomNotesResource().createCustomNote(vmId, request);

        verify(customNotesService,times(1)).createCustomNote(eq(vmId),
                eq("This is a test note created by admin"), eq(username));
    }

    @Test
    public void testCreateCustomNoteCallsCustomNotesServiceForHSAgent() {
        user = GDUserMock.createStaff();
        String username = user.getUsername();
        CustomNotesResource.CustomNoteRequest request = new CustomNotesResource.CustomNoteRequest();
        request.note = "This is a test note created by staff";

        getCustomNotesResource().createCustomNote(vmId, request);

        verify(customNotesService,times(1)).createCustomNote(eq(vmId),
                eq("This is a test note created by staff"), eq(username));
    }
}