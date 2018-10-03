package com.godaddy.vps4.web.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.godaddy.vps4.web.security.Utils.INJECTED_VM_PROPERTY_NAME;
import static com.godaddy.vps4.web.security.Utils.BLOCK_SERVER_TYPE_FILTER_PRIORITY;

import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import org.junit.Test;
import org.junit.Before;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;

import com.godaddy.vps4.vm.ServerType.Type;


public class BlockForServerTypeFilterTest {

    VirtualMachine vm;
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    BlockForServerTypeFilter requestFilter;

    @Before
    public void setupTest() {
        vm = mock(VirtualMachine.class);
        vm.spec = new ServerSpec();
        vm.spec.serverType = new ServerType();
        when(requestContext.getProperty(INJECTED_VM_PROPERTY_NAME)).thenReturn(vm);

        requestFilter = new BlockForServerTypeFilter();
        try {
            Field httpReqField = requestFilter.getClass().getDeclaredField("request");
            httpReqField.setAccessible(true);
            httpReqField.set(requestFilter, httpRequest);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {

        }
    }

    @Test
    public void filterAbortsJaxRSMethodInvocationIfServerTypeMatches() throws Exception {
        vm.spec.serverType.serverType = Type.DEDICATED;
        Type[] serverTypesToBlock = {Type.DEDICATED};
        requestFilter.setServerTypesToBlock(serverTypesToBlock);

        requestFilter.filter(requestContext);

        verify(requestContext, times(1)).abortWith(any(Response.class));
    }

    @Test
    public void filterFallsThroughToJaxRSMethodInvocationIfServerTypeDoesntMatch() throws Exception {
        vm.spec.serverType.serverType = Type.VIRTUAL;
        Type[] serverTypesToBlock = {Type.DEDICATED};
        requestFilter.setServerTypesToBlock(serverTypesToBlock);

        requestFilter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any(Response.class));
    }

    @Test
    public void filterHasAppropriateJaxrsPriority() throws Exception {
        assertTrue(BlockForServerTypeFilter.class.isAnnotationPresent(Priority.class));
        assertEquals(
            BLOCK_SERVER_TYPE_FILTER_PRIORITY, BlockForServerTypeFilter.class.getAnnotation(Priority.class).value());
    }

}