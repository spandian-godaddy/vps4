package com.godaddy.vps4.orchestration.nydus;

import com.godaddy.hfs.sysadmin.SysAdminAction;
import com.godaddy.hfs.sysadmin.SysAdminService;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeNydusTest {
    @Mock private ActionService actionService;
    @Mock private SysAdminService sysAdminService;
    @Mock private CommandContext context;

    private UpgradeNydus command;

    private UpgradeNydus.Request request;
    private final UUID vmId = UUID.randomUUID();
    private final long hfsVmId = 123L;
    private final String version = "1.2.3";

    @Captor private ArgumentCaptor<Function<CommandContext, Void>> updateNydusCaptor;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());
        request = new UpgradeNydus.Request();
        request.vmId = vmId;
        request.hfsVmId = hfsVmId;
        request.version = version;

        command = new UpgradeNydus(actionService, sysAdminService);
    }

    @Test
    public void upgradeNydusExecutesSuccessfully() {
        command.executeWithAction(context, request);

        verify(context, times(1)).execute(eq("UpgradeNydus"), updateNydusCaptor.capture(), any());
        updateNydusCaptor.getValue().apply(context);

        verify(sysAdminService, times(1)).updateNydus(hfsVmId, "upgrade", version);
        verify(context, times(1)).execute(eq("WaitForNydusUpgrade-" + hfsVmId), eq(WaitForSysAdminAction.class), any(SysAdminAction.class));
    }
}
