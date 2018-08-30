package com.godaddy.vps4.web.security;

import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TemporarilyDisabledEndpointFilterTest {

    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    ContainerRequestFilter requestFilter;

    @Before
    public void setupTest() {
        requestFilter = new TemporarilyDisabledEndpointFilter();
    }

    @Test
    public void filterAbortsJaxRSMethodInvocationIfMarkedDisabled() throws Exception {
        requestFilter.filter(requestContext);

        verify(requestContext, times(1)).abortWith(any(Response.class));
    }
}