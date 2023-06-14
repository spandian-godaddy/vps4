package com.godaddy.vps4.web.monitoring;

import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.vm.VmResource;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmAlertResource {

    private final VmResource vmResource;
    private final VmAlertService vmAlertService;
    private final CreditService creditService;

    @Inject
    public VmAlertResource(VmResource vmResource, VmAlertService vmAlertService,
                           CreditService creditService) {
        this.vmResource = vmResource;
        this.vmAlertService = vmAlertService;
        this.creditService = creditService;
    }

    @GET
    @Path("/{vmId}/alerts/")
    public List<VmMetricAlert> getMetricAlertList(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // Auth validation
        VirtualMachineCredit virtualMachineCredit = creditService.getVirtualMachineCredit(vm.orionGuid);
        List<VmMetricAlert> list = vmAlertService.getVmMetricAlertList(vmId);

        list = list.stream().filter(m -> m.metric != VmMetric.HTTPS_DOMAIN
                && m.metric != VmMetric.HTTP_DOMAIN).collect(Collectors.toList());

        if (vm.image.operatingSystem == Image.OperatingSystem.WINDOWS) {
            list = list.stream().filter(m -> m.metric != VmMetric.SSH).collect(Collectors.toList());
        }

        if (virtualMachineCredit.isManaged()) {
            list = list.stream().filter(m -> m.metric != VmMetric.FTP).collect(Collectors.toList());
        }

        return list;
    }

    @GET
    @Path("/{vmId}/alerts/{metric}")
    public VmMetricAlert getMetricAlert(@PathParam("vmId") UUID vmId, @PathParam("metric") String metric) {
        vmResource.getVm(vmId);  // Auth validation
        validateAndReturnEnumValue(VmMetric.class, metric);
        return vmAlertService.getVmMetricAlert(vmId, metric);
    }

    @POST
    @Path("/{vmId}/alerts/{metric}/disable")
    public VmMetricAlert disableMetricAlert(@PathParam("vmId") UUID vmId, @PathParam("metric") String metric) {
        vmResource.getVm(vmId);  // Auth validation
        validateAndReturnEnumValue(VmMetric.class, metric);
        vmAlertService.disableVmMetricAlert(vmId, metric);
        return vmAlertService.getVmMetricAlert(vmId, metric);
    }

    @POST
    @Path("/{vmId}/alerts/{metric}/enable")
    public VmMetricAlert enableMetricAlert(@PathParam("vmId") UUID vmId, @PathParam("metric") String metric) {
        vmResource.getVm(vmId);  // Auth validation
        validateAndReturnEnumValue(VmMetric.class, metric);
        vmAlertService.reenableVmMetricAlert(vmId, metric);
        return vmAlertService.getVmMetricAlert(vmId, metric);
    }

}
