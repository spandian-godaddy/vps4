package com.godaddy.vps4.web.util;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.vm.VirtualMachineService;

public class VmActiveSnapshotFilterTest {

    private UUID vmid = UUID.randomUUID();
    private Long snapshotActionId = 1234L;

    private VirtualMachineService jdbcVmService = Mockito.mock(VirtualMachineService.class);
    private VmActiveSnapshotFilter filter = new VmActiveSnapshotFilter(jdbcVmService);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private FilterChain chain = mock(FilterChain.class);

    @Test
    public void testNonVmApiRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/resource");

        filter.doFilter(request, response, chain);
        verify(jdbcVmService, never()).getPendingSnapshotActionIdByVmId(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiListRequest() throws Exception {
        when(jdbcVmService.getPendingSnapshotActionIdByVmId(vmid)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/vms/");

        filter.doFilter(request, response, chain);
        verify(jdbcVmService, never()).getPendingSnapshotActionIdByVmId(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiWithoutPendingSnapshot() throws Exception {
        when(jdbcVmService.getPendingSnapshotActionIdByVmId(vmid)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/vms/" + vmid);

        filter.doFilter(request, response, chain);
        verify(jdbcVmService, times(1)).getPendingSnapshotActionIdByVmId(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiGetWithPendingSnapshot() throws Exception {
        when(jdbcVmService.getPendingSnapshotActionIdByVmId(vmid)).thenReturn(snapshotActionId);
        when(request.getRequestURI()).thenReturn("/api/vms/" + vmid);
        when(request.getMethod()).thenReturn("GET");

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiPatchWithPendingSnapshot() throws Exception {
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        when(jdbcVmService.getPendingSnapshotActionIdByVmId(vmid)).thenReturn(snapshotActionId);
        when(request.getRequestURI()).thenReturn("/api/vms/" + vmid);
        when(request.getMethod()).thenReturn("PATCH");

        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        JSONObject json = (JSONObject) new JSONParser().parse(writer.toString());
        Assert.assertEquals("SNAPSHOT_ACTION_IN_PROGRESS", json.get("id"));
    }

    @Test
    public void testVmApiPostWithPendingSnapshot() throws Exception {
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        when(jdbcVmService.getPendingSnapshotActionIdByVmId(vmid)).thenReturn(snapshotActionId);
        when(request.getRequestURI()).thenReturn("/api/vms/" + vmid + "/start");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        JSONObject json = (JSONObject) new JSONParser().parse(writer.toString());
        Assert.assertEquals("SNAPSHOT_ACTION_IN_PROGRESS", json.get("id"));
   }

   @Test
   public void initDoesNothing() throws Exception {
       FilterConfig fc = mock(FilterConfig.class);
       filter.init(fc);
   }

   @Test
   public void destroyDoesNothing() throws Exception {
       filter.destroy();
   }
}
