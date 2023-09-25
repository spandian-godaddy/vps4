package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CommandMetadata(
        name = "Vps4MoveIn",
        requestType = Vps4MoveIn.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class Vps4MoveIn extends ActionCommand<Vps4MoveIn.Request, Void> {

    private final CreditService creditService;
    private final ActionService actionService;
    private static final Logger logger = LoggerFactory.getLogger(Vps4MoveIn.class);

    @Inject
    public Vps4MoveIn(ActionService actionService, CreditService creditService) {
        super(actionService);
        this.creditService = creditService;
        this.actionService = actionService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) throws Exception {
        try {
            updateProdMeta(context, request);
            resumePanoptaMonitoring(context, request);
            insertActionRecords(context, request.vm.vmId, request.actions);
        } catch (Exception e) {
            logger.error("Vps4MoveIn failed for vmId {}", request.vm.vmId);
            throw e;
        }

        return null;
    }

    private void updateProdMeta(CommandContext context, Request request) {
        Map<ECommCreditService.ProductMetaField, String> newProdMeta = new EnumMap<>(ECommCreditService.ProductMetaField.class);
        newProdMeta.put(ECommCreditService.ProductMetaField.DATA_CENTER, String.valueOf(request.vm.dataCenter.dataCenterId));
        newProdMeta.put(ECommCreditService.ProductMetaField.PRODUCT_ID, request.vm.vmId.toString());
        context.execute("UpdateProdMeta", ctx -> {
            creditService.updateProductMeta(request.vm.orionGuid, newProdMeta);
            return null;
        }, Void.class);
    }

    private static void resumePanoptaMonitoring(CommandContext context, Request request) {
        context.execute(ResumePanoptaMonitoring.class, request.vm);
    }

    private void insertActionRecords(CommandContext context, UUID vmId, List<Action> actions) {
        context.execute("MoveInActions", ctx -> {
            for (Action action : actions) {
                try {
                    actionService.insertAction(vmId, action);
                } catch (Exception e) {
                    logger.warn("Failed to insert actionId {} for vmId {}. Skipping this one", action.id, vmId, e);
                }
            }
            return null;
        }, Void.class);
    }

    public static class Request extends Vps4ActionRequest {
        public VirtualMachine vm;
        public List<Action> actions;
    }
}
