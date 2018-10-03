package com.godaddy.vps4.web.security;

import static com.godaddy.vps4.web.security.Utils.LOOKUP_VM_FILTER_PRIORITY;
import static com.godaddy.vps4.web.security.Utils.INJECTED_VM_PROPERTY_NAME;
import static com.godaddy.vps4.web.security.Utils.isVmDetailsEndpoint;
import static com.godaddy.vps4.web.security.Utils.parseVmIdFromURI;

import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;


@Priority(LOOKUP_VM_FILTER_PRIORITY)
public class LookupVmFilter implements ContainerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(LookupVmFilter.class);

    final VirtualMachineService virtualMachineService;

    @Context
    private HttpServletRequest request;

    @Inject
    public LookupVmFilter(VirtualMachineService virtualMachineService) {
        this.virtualMachineService = virtualMachineService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String requestUri = request.getRequestURI();
        logger.debug("Executing {} for endpoint: {}", this.getClass().getSimpleName(), request.getRequestURI());
        if (isVmDetailsEndpoint(requestUri)) {
            UUID vmId = parseVmIdFromURI(requestUri);
            try {
                VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
                logger.debug("Found vm {}", vm.vmId);
                requestContext.setProperty(INJECTED_VM_PROPERTY_NAME, vm);
            }
            catch (Exception ex) {
                requestContext.abortWith(Response.status(Response.Status.NOT_FOUND).build());
            }
        }
    }
}