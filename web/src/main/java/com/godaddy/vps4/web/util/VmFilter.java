package com.godaddy.vps4.web.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;

public abstract class VmFilter implements Filter{


    // Regex matches vm api requests and captures vmid and vm endpoint used, Ex: /api/vms/<vmId>/<endpoint>/endpointId/sub-endpoint
    private static final Pattern VM_API_PATTERN = Pattern.compile("/api/vms/(?<vmid>[0-9a-fA-F-]+)/?(?<endpoint>.*?)(?:/.*)*");
    protected final List<String> excludedVmEndpoints;
    final VirtualMachineService virtualMachineService;

    public VmFilter(List<String> excludedVmEndpoints, VirtualMachineService virtualMachineService) {
        this.excludedVmEndpoints = excludedVmEndpoints;
        this.virtualMachineService = virtualMachineService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}

    @Override
    @SuppressWarnings("unchecked")
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        try {
            validateSafeApiRequest(request);
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

    protected void validateSafeApiRequest(HttpServletRequest request) {
        String httpMethod = request.getMethod();
        List<String> unsafeHttpMethods = Arrays.asList("POST", "PUT", "PATCH");
        if (unsafeHttpMethods.contains(httpMethod)) {
            validateIfVmApiRequest(request);
        }
    }

    protected void validateIfVmApiRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        Matcher m = VM_API_PATTERN.matcher(requestUri);
        if (m.matches() && !isVmEndpointExcluded(m.group("endpoint"))) {
            String vmId = m.group("vmid");
            doFilterSpecificFiltering(request, UUID.fromString(vmId));
        }
    }

    private boolean isVmEndpointExcluded(String endpoint) {
        return excludedVmEndpoints.contains(endpoint);
    }

    protected abstract void doFilterSpecificFiltering(HttpServletRequest request, UUID vmId);
}
