package com.godaddy.vps4.web.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;

/**
 * If a VM API request includes a vmId, then verify that there are no active snapshot actions for that VM.
 * Return 400 response if snapshot action pending, otherwise allow the request to continue.
 */
public class VmActiveSnapshotFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(VmActiveSnapshotFilter.class);

    private static final String MATCH_PATTERN = "/api/vms/(?<vmid>[0-9a-f-]+)/?.*";
    private static final String EXCLUDE_PATTERN = "/api/vms/(?<vmid>[0-9a-f-]+)/messaging/?.*";

    final VirtualMachineService virtualMachineService;

    @Inject
    public VmActiveSnapshotFilter(VirtualMachineService virtualMachineService) {
        this.virtualMachineService = virtualMachineService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}

    @Override
    @SuppressWarnings("unchecked")
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        try {
            validateIfVmApiRequest(request);
            chain.doFilter(req, res);
        }
        catch (Vps4Exception e) {
            JSONObject json = new JSONObject();
            json.put("id", e.getId());
            json.put("message", e.getMessage());

            HttpServletResponse response = (HttpServletResponse) res;
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            try (Writer writer = response.getWriter()) {
                writer.write(json.toJSONString());
            }
        }
    }

    private void validateIfVmApiRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        if(shouldEndpointBeExcluded(requestUri)) {
            return;
        }

        Matcher m = Pattern.compile(MATCH_PATTERN).matcher(requestUri);
        if (m.matches()) {
            String vmId = m.group("vmid");
            validateIfVmHasActiveSnapshotAction(request, vmId);
        }
    }

	private boolean shouldEndpointBeExcluded(String requestUri) {
		return Pattern.matches(EXCLUDE_PATTERN, requestUri);
	}

    private void validateIfVmHasActiveSnapshotAction(HttpServletRequest request, String vmId) {
        Long snapshotActionId = getPendingSnapshotAction(UUID.fromString(vmId));
        if (snapshotActionId != null)
            validateHttpMethodAllowedDuringSnapshot(request, snapshotActionId);
    }

    private Long getPendingSnapshotAction(UUID vmId) {
        return virtualMachineService.getPendingSnapshotActionIdByVmId(vmId);
    }

    private void validateHttpMethodAllowedDuringSnapshot(HttpServletRequest request, Long actionId) {
        String httpMethod = request.getMethod();
        List<String> disallowedHttpMethods = Arrays.asList("POST", "PUT", "PATCH");
        if (disallowedHttpMethods.contains(httpMethod)) {
            String errorMsg = "Request not allowed while snapshot action running";
            logger.info(errorMsg + String.format(", action: %s, request: %s %s",
                    actionId, httpMethod, request.getRequestURI()));
            throw new Vps4Exception("SNAPSHOT_ACTION_IN_PROGRESS", errorMsg);
        }
    }

}
