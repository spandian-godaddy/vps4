package com.godaddy.vps4.web.network;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import junit.framework.Assert;

public class NetworkResourceTest {

    private UUID vmId;
    private NetworkResource resource;
    private ActionService actionService = mock(ActionService.class);
    private NetworkService networkService = mock(NetworkService.class);
    private VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    private VirtualMachine vm;
    private Vps4User vps4User;
    private VmResource vmResource = mock(VmResource.class);


    @Before
    public void setupTest(){
        vmId = UUID.randomUUID();
        long hfsVmId = 1111;

        GDUser user = GDUserMock.createShopper();
        ProjectService projectService = mock(ProjectService.class);
        CommandService commandService = mock(CommandService.class);
        Config config = mock(Config.class);

        ServerType vmServerType = new ServerType();
        vmServerType.platform = ServerType.Platform.OPTIMIZED_HOSTING;
        ServerSpec vmSpec = new ServerSpec();
        vmSpec.serverType = vmServerType;
        vm = new VirtualMachine(vmId, hfsVmId, UUID.randomUUID(),
                1, vmSpec, "Unit Test Vm", null, null,
                Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS), Instant.now().plus(24, ChronoUnit.HOURS),
                null, null, 0, UUID.randomUUID());

        vps4User = new Vps4User(112, user.getShopperId());
        when(virtualMachineService.getUserIdByVmId(vmId)).thenReturn(vps4User.getId());

        Project project = new Project(123, "unitTestProject", "vps4-unittest-123", Instant.now(), null);
        when(projectService.getProject(vm.projectId)).thenReturn(project);

        CommandState commandState = new CommandState();
        commandState.commandId = null;
        when(commandService.executeCommand(any(CommandGroupSpec.class))).thenReturn(commandState);

        when(vmResource.getVm(vmId)).thenReturn(vm);

        resource = new NetworkResource(user, networkService, actionService, virtualMachineService,
                projectService, commandService, vmResource, config);

    }
    @Test
    public void testReturnsActionForOH(){
        Action action = new Action(123, UUID.randomUUID(), ActionType.ADD_IP,
                "{}", "NEW", "{}", ActionStatus.NEW, Instant.now(), null, "", UUID.randomUUID(), "tester");
        when(actionService.getAction(anyLong())).thenReturn(action);
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(vm);

        resource.addIpAddress(vmId);
        verify(actionService, times(1)).createAction(eq(vm.vmId), eq(ActionType.ADD_IP), anyString(), anyString());


    }
    @Test
    public void testReturnsActionForOVHServer(){
        ServerType vmServerType = new ServerType();
        vmServerType.platform = ServerType.Platform.OVH;
        ServerSpec vmSpec = new ServerSpec();
        vmSpec.serverType = vmServerType;
        vm = new VirtualMachine(vmId, 1111, UUID.randomUUID(),
                1, vmSpec, "Unit Test Vm", null, null,
                Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS), Instant.now().plus(24, ChronoUnit.HOURS),
                null, null, 0, UUID.randomUUID());

        Action action = new Action(123, UUID.randomUUID(), ActionType.ADD_IP,
        "{}", "NEW", "{}", ActionStatus.NEW, Instant.now(), null, "", UUID.randomUUID(), "tester");
        when(actionService.getAction(anyLong())).thenReturn(action);
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(vm);
        resource.addIpAddress(vmId);
        verify(actionService, times(1)).createAction(eq(vm.vmId), eq(ActionType.ADD_IP), anyString(), anyString());


    }

    @Test
    public void testDestroyIp(){
        Action action = new Action(123, UUID.randomUUID(), ActionType.DESTROY_IP,
                "{}", "NEW", "{}", ActionStatus.NEW, Instant.now(), null, "", UUID.randomUUID(), "tester");
        when(actionService.createAction(vm.vmId, ActionType.DESTROY_IP, new JSONObject().toJSONString(), action.initiatedBy)).thenReturn(action.id);
        when(actionService.getAction(action.id)).thenReturn(action);

        IpAddress ip = new IpAddress(1111, vmId, "1.2.3.4", IpAddressType.SECONDARY,
                null, Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS));
        when(networkService.getIpAddress(1111)).thenReturn(ip);
        Action returnAction = resource.destroyIpAddress(vmId, 1111);
        Assert.assertEquals(action, returnAction);
    }

    @Test(expected=Vps4Exception.class)
    public void testAddIpOpenStackNotAllowed(){
        ServerType serverType = new ServerType();
        serverType.platform = ServerType.Platform.OPENSTACK;
        ServerSpec spec = new ServerSpec();
        spec.serverType = serverType;
        VirtualMachine vm = new VirtualMachine(vmId, 1111, UUID.randomUUID(),
                1, spec , "Unit Test Vm", null, null,
                Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS), Instant.now().plus(24, ChronoUnit.HOURS),
                null, null, 0, UUID.randomUUID());
        when(vmResource.getVm(vmId)).thenReturn(vm);
        resource.addIpAddress(vmId);
    }

    @Test(expected=NotFoundException.class)
    public void testAddIpVmNotTiedToHfs(){
        ServerType serverType = new ServerType();
        serverType.platform = ServerType.Platform.OPTIMIZED_HOSTING;
        ServerSpec spec = new ServerSpec();
        spec.serverType = serverType;
        VirtualMachine vm = new VirtualMachine(vmId, 0, UUID.randomUUID(),
                1, spec , "Unit Test Vm", null, null,
                Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS), Instant.now().plus(24, ChronoUnit.HOURS),
                null, null, 0, UUID.randomUUID());
        when(vmResource.getVm(vmId)).thenReturn(vm);
        resource.addIpAddress(vmId);
    }

    @Test(expected=NotFoundException.class)
    public void testDestroyIpNotFound(){
        when(networkService.getIpAddress(1111)).thenReturn(null);
        resource.destroyIpAddress(vmId, 1111);
    }

    @Test(expected=NotFoundException.class)
    public void testDestroyIpBelongsToDifferentVm(){
        IpAddress ip = new IpAddress(1111, UUID.randomUUID(), "1.2.3.4", IpAddressType.SECONDARY,
                null, Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS));

        when(networkService.getIpAddress(1111)).thenReturn(ip);
        resource.destroyIpAddress(vmId, 1111);
    }

    @Test(expected=NotFoundException.class)
    public void testDestroyIpAlreadyRemoved(){
        IpAddress ip = new IpAddress(1111, vmId, "1.2.3.4", IpAddressType.SECONDARY,
                null, Instant.now(), Instant.now().minus(24, ChronoUnit.HOURS));

        when(networkService.getIpAddress(1111)).thenReturn(ip);
        resource.destroyIpAddress(vmId, 1111);
    }

    @Test(expected=Vps4Exception.class)
    public void testDeletePrimaryIp(){
        IpAddress ip = new IpAddress(1111, vmId, "1.2.3.4", IpAddressType.PRIMARY, null, Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS));

        when(networkService.getIpAddress(1111)).thenReturn(ip);
        resource.destroyIpAddress(vmId, 1111);
    }

    @Test
    public void testGetIpAddresses(){
        IpAddress primaryIp = new IpAddress(1111, vmId, "1.2.3.4", IpAddressType.PRIMARY,
                null, Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS));
        IpAddress secondaryIp = new IpAddress(1111, vmId, "1.2.3.4", IpAddressType.SECONDARY,
                null, Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS));
        IpAddress removedIp = new IpAddress(1111, vmId, "1.2.3.4", IpAddressType.SECONDARY,
                null, Instant.now(), Instant.now().minus(24, ChronoUnit.HOURS));

        List<IpAddress> allIps = new ArrayList<IpAddress>();
        allIps.add(primaryIp);
        allIps.add(secondaryIp);
        allIps.add(removedIp);

        List<IpAddress> expectedReturn = new ArrayList<IpAddress>();
        expectedReturn.add(primaryIp);
        expectedReturn.add(secondaryIp);

        when(networkService.getVmIpAddresses(vmId)).thenReturn(allIps);

        List<IpAddress> actualReturn = resource.getIpAddresses(vmId);

        Assert.assertEquals(expectedReturn, actualReturn);
    }

}
