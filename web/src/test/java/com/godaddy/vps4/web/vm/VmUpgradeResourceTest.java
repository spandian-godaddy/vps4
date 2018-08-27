package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.credit.CreditModule;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmUpgradeResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VmUpgradeResourceTest {

    private GDUser user;
    private VirtualMachineService virtualMachineService;
    private CreditService creditService;
    private ActionService actionService;

    private VmUpgradeResource resource;

    private VirtualMachineCredit testCredit;
    private VirtualMachine testVm;


    @Before
    public void setupTest() {
        user = GDUserMock.createShopper();

        testCredit = new VirtualMachineCredit();
        testCredit.accountStatus = AccountStatus.ACTIVE;
        testCredit.planChangePending = true;
        testCredit.orionGuid = UUID.randomUUID();
        testCredit.shopperId = user.getShopperId();

        testVm = new VirtualMachine();
        testVm.validOn = Instant.now();
        testVm.canceled = Instant.MAX;
        testVm.validUntil = Instant.MAX;
        testVm.vmId = UUID.randomUUID();
        testVm.orionGuid = testCredit.orionGuid;

        virtualMachineService = mock(VirtualMachineService.class);
        creditService = mock(CreditService.class);
        actionService = mock(ActionService.class);

        when(virtualMachineService.getVirtualMachine(testVm.vmId)).thenReturn(testVm);
        when(actionService.getActions(Matchers.eq(testVm.vmId), Matchers.eq(-1), Matchers.eq(0), anyList()))
                .thenReturn(new ResultSubset<Action>(null, 0));

        resource = new VmUpgradeResource(user, virtualMachineService, creditService, actionService);
    }

    @Test
    public void testUpgradeVm() {
        when(creditService.getVirtualMachineCredit(testCredit.orionGuid)).thenReturn(testCredit);

        resource.upgradeVm(testVm.vmId);
    }

    @Test(expected = Vps4Exception.class)
    public void testUpgradeVmNoPlanChangePending() {
        testCredit.planChangePending = false;
        when(creditService.getVirtualMachineCredit(testCredit.orionGuid)).thenReturn(testCredit);

        resource.upgradeVm(testVm.vmId);
    }

}
