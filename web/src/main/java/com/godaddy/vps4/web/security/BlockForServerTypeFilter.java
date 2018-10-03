package com.godaddy.vps4.web.security;

import static com.godaddy.vps4.web.security.Utils.BLOCK_SERVER_TYPE_FILTER_PRIORITY;
import static com.godaddy.vps4.web.security.Utils.INJECTED_VM_PROPERTY_NAME;
import static com.godaddy.vps4.web.security.Utils.Status;

import com.godaddy.vps4.vm.ServerType.Type;
import com.godaddy.vps4.vm.VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;


@Priority(BLOCK_SERVER_TYPE_FILTER_PRIORITY)
public class BlockForServerTypeFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(BlockForServerTypeFilter.class);

    @Context
    private HttpServletRequest request;

    Type[] blockedServerTypes;

    public void setServerTypesToBlock(Type[] blockedServerTypes) {
        this.blockedServerTypes = blockedServerTypes;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        logger.debug("Executing {} for endpoint: {}", this.getClass().getSimpleName(), request.getRequestURI());

        VirtualMachine vm = (VirtualMachine) requestContext.getProperty(INJECTED_VM_PROPERTY_NAME);
        logger.debug(String.format("Server type: %s, Block serverTypes: %s",
            vm.spec.serverType, Arrays.toString(blockedServerTypes)));

        if (shouldBlockServerType(vm.spec.serverType.serverType)) {
            requestContext.abortWith(Response.status(Status.UNPROCESSABLE_ENTITY).build());
        }
    }

    private boolean shouldBlockServerType(Type serverType) {
        return Arrays.stream(blockedServerTypes).anyMatch(r -> r.equals(serverType));
    }

}