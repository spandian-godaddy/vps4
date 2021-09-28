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
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.vm.VirtualMachineService;

public class ImportedVmFilterTest {

    private UUID importedVmId = UUID.randomUUID();
    private UUID notImportedVmId = UUID.randomUUID();

    private VirtualMachineService jdbcVmService = mock(VirtualMachineService.class);
    private ImportedVmFilter filter = new ImportedVmFilter(jdbcVmService);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private FilterChain chain = mock(FilterChain.class);
    private StringWriter writer;

    @Before
    public void setUp() throws Exception {
        writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        when(jdbcVmService.getImportedVm(importedVmId)).thenReturn(importedVmId);
        when(jdbcVmService.getImportedVm(notImportedVmId)).thenReturn(null);
        when(request.getMethod()).thenReturn("POST");
    }

    @Test
    public void testNonVmApiRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/resource");

        filter.doFilter(request, response, chain);
        verify(jdbcVmService, never()).getImportedVm(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiListRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/vms/");

        filter.doFilter(request, response, chain);
        verify(jdbcVmService, never()).getImportedVm(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiNotImportedVmCallsGetImportedVm() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/vms/" + notImportedVmId + "/enableAdmin");

        filter.doFilter(request, response, chain);
        verify(jdbcVmService, times(1)).getImportedVm(notImportedVmId);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiGetImportedVm() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/vms/" + importedVmId);
        when(request.getMethod()).thenReturn("GET");

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiDeleteImportedVm() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/vms/" + importedVmId);
        when(request.getMethod()).thenReturn("DELETE");

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiReviveImportedVm() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/vms/" + importedVmId + "/revive");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiZombieImportedVm() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/vms/" + importedVmId + "/zombie");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiPatchImportedVm() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/vms/" + importedVmId);
        when(request.getMethod()).thenReturn("PATCH");

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testPendingImportedVmUpperCaseUUID() throws Exception {
        String upperVmid = importedVmId.toString().toUpperCase();
        when(request.getRequestURI()).thenReturn("/api/vms/" + upperVmid + "/enableAdmin");

        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        JSONObject json = (JSONObject) new JSONParser().parse(writer.toString());
        Assert.assertEquals("BLOCKED_FOR_IMPORTED_VM", json.get("id"));
   }

    @Test
    public void testExcludedMethodWithImportedVm() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/vms/" + importedVmId + "/start");

        filter.doFilter(request, response, chain);
        verify(jdbcVmService, never()).getPendingSnapshotActionIdByVmId(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testVmApiSetPasswordImportedVm() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/vms/" + importedVmId + "/setPassword");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
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
