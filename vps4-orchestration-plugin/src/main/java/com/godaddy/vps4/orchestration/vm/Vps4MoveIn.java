package com.godaddy.vps4.orchestration.vm;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.monitoring.Vps4AddMonitoring;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4MoveIn",
        requestType = Vps4MoveIn.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class Vps4MoveIn extends ActionCommand<Vps4MoveIn.Request, Void> {
    private final CreditService creditService;
    private final ActionService actionService;
    private static final Logger logger = LoggerFactory.getLogger(Vps4MoveIn.class);

    private CommandContext context;
    private Request request;

    @Inject
    public Vps4MoveIn(ActionService actionService, CreditService creditService) {
        super(actionService);
        this.creditService = creditService;
        this.actionService = actionService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) {
        this.context = context;
        this.request = request;

        try {
            updateProdMeta();
            insertActionRecords();
            installPanopta();
        } catch (Exception e) {
            logger.error("Vps4MoveIn failed for vmId {}", request.virtualMachine.vmId);
            throw e;
        }

        return null;
    }

    private void updateProdMeta() {
        Map<ECommCreditService.ProductMetaField, String> newProdMeta = new EnumMap<>(ECommCreditService.ProductMetaField.class);
        newProdMeta.put(ECommCreditService.ProductMetaField.DATA_CENTER,
                        String.valueOf(request.virtualMachine.dataCenter.dataCenterId));
        newProdMeta.put(ECommCreditService.ProductMetaField.PRODUCT_ID,
                        request.virtualMachine.vmId.toString());
        context.execute("UpdateProdMeta", ctx -> {
            creditService.updateProductMeta(request.virtualMachine.orionGuid, newProdMeta);
            return null;
        }, Void.class);
    }

    private void insertActionRecords() {
        context.execute("MoveInActions", ctx -> {
            for (Action action : request.actions) {
                actionService.insertAction(request.virtualMachine.vmId, action);
            }
            return null;
        }, Void.class);
    }

    private void installPanopta() {
        try {
            context.execute(Vps4AddMonitoring.class, request);
        } catch (Exception e) {
            logger.error("Exception while setting up Panopta for migrated VM {}: {}", request.virtualMachine.vmId, e);
        }
    }

    public static class Request extends VmActionRequest {
        public List<Action> actions;
    }
}
