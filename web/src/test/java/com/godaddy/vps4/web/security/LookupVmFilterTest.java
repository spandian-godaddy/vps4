package com.godaddy.vps4.web.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.godaddy.vps4.web.security.Utils.INJECTED_VM_PROPERTY_NAME;
import static com.godaddy.vps4.web.security.Utils.LOOKUP_VM_FILTER_PRIORITY;

import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import org.junit.Test;
import org.junit.Before;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.UUID;


public class LookupVmFilterTest {

    UUID vmId = UUID.randomUUID();
    VirtualMachine vm;
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    LookupVmFilter requestFilter;
    String requestUri;
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);

    @Before
    public void setupTest() {
        requestUri = String.format("/api/vms/%s/foo/bar", vmId);
        vm = mock(VirtualMachine.class);
        when(httpRequest.getRequestURI()).thenReturn(requestUri);
        when(virtualMachineService.getVirtualMachine(eq(vmId))).thenReturn(vm);

        requestFilter = new LookupVmFilter(virtualMachineService);
        try {
            Field httpReqField = requestFilter.getClass().getDeclaredField("request");
            httpReqField.setAccessible(true);
            httpReqField.set(requestFilter, httpRequest);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {

        }
    }

    @Test
    public void filterSetsRequestPropertyIfVmFound() throws Exception {
        requestFilter.filter(requestContext);
        verify(virtualMachineService, times(1)).getVirtualMachine(vmId);
        verify(requestContext, times(1)).setProperty(INJECTED_VM_PROPERTY_NAME, vm);
    }

    @Test
    public void filterFallsThroughToJaxRSMethodInvocationIfVmFound() throws Exception {
        requestFilter.filter(requestContext);
        verify(requestContext, times(0)).abortWith(any(Response.class));
    }

    @Test
    public void filterFallsThroughToJaxRSMethodInvocationIfNonVmRequest() throws Exception {
        when(httpRequest.getRequestURI()).thenReturn("/api/vms"); // not request for a particular vm
        requestFilter.filter(requestContext);
        verify(virtualMachineService, times(0)).getVirtualMachine(any(UUID.class));
        verify(requestContext, times(0)).abortWith(any(Response.class));
    }

    @Test
    public void filterAbortsJaxRSMethodInvocationIfVmNotFound() throws Exception {
        when(virtualMachineService.getVirtualMachine(eq(vmId))).thenThrow(new RuntimeException());
        requestFilter.filter(requestContext);

        verify(requestContext, times(1)).abortWith(any(Response.class));
    }

    @Test
    public void filterHasAppropriateJaxrsPriority() throws Exception {
        assertTrue(LookupVmFilter.class.isAnnotationPresent(Priority.class));
        assertEquals(
                LOOKUP_VM_FILTER_PRIORITY, LookupVmFilter.class.getAnnotation(Priority.class).value());
    }

}