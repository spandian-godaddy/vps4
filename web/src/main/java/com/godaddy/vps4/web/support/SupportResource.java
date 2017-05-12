package com.godaddy.vps4.web.support;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.PaginatedResult;
import com.godaddy.vps4.web.Vps4Api;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags={ "support" })

@Path("/api/support")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SupportResource {

    private static final Logger logger = LoggerFactory.getLogger(SupportResource.class);

    private final ActionService actionService;
    private final VirtualMachineService virtualMachineService;
    private final CreditService creditService;
    private final Vps4UserService vps4UserService;
    private final CommandService commandService;

    @Inject
    public SupportResource(ActionService actionService, VirtualMachineService virtualMachineService,
            CreditService creditService, Vps4UserService vps4UserService, CommandService commandService){
        this.actionService = actionService;
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.vps4UserService = vps4UserService;
        this.commandService = commandService;
    }

    @GET
    @Path("vms")
    public List<VirtualMachine> getVms(@QueryParam("shopperId") String shopperId) {

        logger.info("getting vms with shopper id {}", shopperId);
        Vps4User vps4User = vps4UserService.getUser(shopperId);
        if(vps4User == null){
            throw new NotFoundException("Unknown shopper id: " + shopperId);
        }
        return virtualMachineService.getVirtualMachinesForUser(vps4User.getId());
    }

    @GET
    @Path("vms/{vmId}")
    public VirtualMachine getVm(@PathParam("vmId") UUID vmId) {
        logger.info("getting vm with id {}", vmId);
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);

        if (virtualMachine == null || virtualMachine.validUntil.isBefore(Instant.now())) {
            throw new NotFoundException("Unknown VM ID: " + vmId);
        }

        return virtualMachine;
    }

    @GET
    @Path("actions/{actionId}")
    public SupportAction getAction(@PathParam("actionId") long actionId) {

        Action action = actionService.getAction(actionId);

        if (action == null) {
            throw new NotFoundException("actionId " + actionId + " not found");
        }

        CommandState commandState = null;
        if(action.commandId != null){
            commandState = this.commandService.getCommand(action.commandId);
        }

        return new SupportAction(action, commandState);
    }

    private Date convertDate(String strDate) throws ParseException{
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss z");
        Date resultDate = null;
        if(strDate != null){
            resultDate = df.parse(strDate);
        }
        return resultDate;
    }

    @GET
    @Path("actions")
    public PaginatedResult<SupportAction> getActions(@QueryParam("vmId") UUID vmId,
            @DefaultValue("10") @QueryParam("limit") long limit,
            @DefaultValue("0") @QueryParam("offset") long offset,
            @QueryParam("status") List<String> status,
            @ApiParam(value = "Earliest date/time the action was created.  Format = yyyy-MM-dd hh:mm:ss timezone", example = "2017-01-01 01:02:03 GMT") @QueryParam("beginTime") String beginTime,
            @ApiParam(value = "Latest date/time the action was created.  Format = yyyy-MM-dd hh:mm:ss timezone", example = "2017-03-20 01:02:03 GMT") @QueryParam("endTime") String endTime,
            @DefaultValue("false") @QueryParam("includeCommandData") boolean includeCommandData,
            @Context UriInfo uri) throws ParseException {

        Date beginDate = convertDate(beginTime);
        Date endDate = convertDate(endTime);

        ResultSubset<Action> actions = actionService.getActions(vmId, limit, offset, status, beginDate, endDate);

        List<SupportAction> supportActions = new ArrayList<SupportAction>();

        long totalRows = 0;
        if (actions != null) {
            totalRows = actions.totalRows;

            for(Action action : actions.results){
                if(action.commandId != null && includeCommandData){
                    supportActions.add(new SupportAction(action, this.commandService.getCommand(action.commandId)));
                }else{
                    supportActions.add(new SupportAction(action, null));
                }
            }
        }
        return new PaginatedResult<SupportAction>(supportActions, limit, offset, totalRows, uri);
    }

    @POST
    @Path("/createCredit")
    public VirtualMachineCredit createCredit(CreateCreditRequest request){
        UUID orionGuid = UUID.randomUUID();

        creditService.createVirtualMachineCredit(orionGuid,
                request.operatingSystem, request.controlPanel,
                request.tier, request.managedLevel, request.monitoring, request.shopperId);

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        return credit;
    }

    public static class CreateCreditRequest {
        public int tier;
        public int managedLevel;
        public int monitoring;
        public String operatingSystem;
        public String controlPanel;
        public String shopperId;
    }

}
