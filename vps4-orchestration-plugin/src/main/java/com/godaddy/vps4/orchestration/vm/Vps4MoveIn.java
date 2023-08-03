package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
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
import java.util.Map;
import java.util.UUID;

@CommandMetadata(
        name = "Vps4MoveIn",
        requestType = Vps4MoveOut.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class Vps4MoveIn extends ActionCommand<Vps4MoveIn.Request, Void> {

    private CommandContext context;
    private final CreditService creditService;
    private static final Logger logger = LoggerFactory.getLogger(Vps4MoveIn.class);

    @Inject
    public Vps4MoveIn(ActionService actionService, CreditService creditService) {
        super(actionService);
        this.creditService = creditService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) throws Exception {

        try {
            updateProdMeta(request);
            resumePanoptaMonitoring(context, request);
        } catch (Exception e) {
            logger.error("Vps4MoveIn failed for vmId {}", request.vm.vmId);
            throw e;
        }

        return null;
    }

    private void updateProdMeta(Request request) {
        Map<ECommCreditService.ProductMetaField, String> newProdMeta = new EnumMap<>(ECommCreditService.ProductMetaField.class);
        newProdMeta.put(ECommCreditService.ProductMetaField.DATA_CENTER, String.valueOf(request.vm.dataCenter.dataCenterId));
        newProdMeta.put(ECommCreditService.ProductMetaField.PROVISION_DATE, Instant.now().toString());
        newProdMeta.put(ECommCreditService.ProductMetaField.PRODUCT_ID, request.vm.vmId.toString());
        creditService.updateProductMeta(request.vm.orionGuid, newProdMeta);
    }

    private static void resumePanoptaMonitoring(CommandContext context, Request request) {
        context.execute(ResumePanoptaMonitoring.class, request.vm);
    }

    public static class Request extends Vps4ActionRequest {
        public VirtualMachine vm;
    }
}
