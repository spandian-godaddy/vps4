package com.godaddy.vps4.web.security;

import com.godaddy.vps4.web.Vps4Api;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Utils {
    private static final String VM_URI_PATTERN = "/api/vms/(?<vmid>[0-9a-f-]+)/?.*";
    public static final Pattern vmPatternMatcher = Pattern.compile(VM_URI_PATTERN);

    // Jax-rs Request context property names
    public static final String INJECTED_VM_PROPERTY_NAME = "request-vm";
    public static final String INJECTED_CREDIT_PROPERTY_NAME = "request-credit";

    // Request filter priorities
    public static final int ADMIN_AUTH_FILTER_PRIORITY = 2010;
    public static final int REQUIRES_ROLE_FILTER_PRIORITY = 2020;
    public static final int LOOKUP_VM_FILTER_PRIORITY = 2030;
    public static final int SHOPPER_ACCESS_FILTER_PRIORITY = 2040;
    public static final int BLOCK_SERVER_TYPE_FILTER_PRIORITY = 2050;

    // Details endpoint here means anything that hangs off /api/vms/:vmId/...
    // irrespective of the HTTP method.
    public static boolean isVmDetailsEndpoint(String requestUri) {
        Matcher m = vmPatternMatcher.matcher(requestUri);
        return m.matches();
    }

    public static UUID parseVmIdFromURI(String requestUri) {
        Matcher m = vmPatternMatcher.matcher(requestUri);
        return m.matches()
            ? UUID.fromString(m.group("vmid"))
            : null;
    }

    // check if this resource method services a request to an endpoint that begins with /api/vms/{vmId},
    // basically if this request is related to a 'specific' vm.
    public static boolean isAVmResourceMethod(Class<?> resourceCls, Method resourceMethod) {
        return isVps4ApiVmResource(resourceCls) && methodHasVmIdPathParam(resourceMethod);
    }

    // Check if there is any param in the resource method that has a Jax-rs PathParam annotation whose value is 'vmId'
    private static boolean methodHasVmIdPathParam(Method resourceMethod) {
        return Arrays.stream(resourceMethod.getParameterAnnotations()) // Get all the annotations on every method param
            .anyMatch(annotations -> Arrays.stream(annotations) // For each list of annotations per param
                .anyMatch(annotation -> annotation.annotationType().equals(PathParam.class) // Check if annotation is of type PathParam
                    && ((PathParam) annotation).value().equals("vmId"))); // and if the value of this annotation is vmId
    }

    public static boolean isVps4ApiResourceMethod(Class<?> resourceCls) {
        return resourceCls.isAnnotationPresent(Vps4Api.class);
    }

    // Check if the resource represents anything that hangs off the path '/api/vms/...'
    public static boolean isVps4ApiVmResource(Class<?> resourceCls) {
        return isVps4ApiResourceMethod(resourceCls)
            && resourceCls.isAnnotationPresent(Path.class)
            && resourceCls.getAnnotation(Path.class).value().equals("/api/vms");
    }


    public enum Status implements Response.StatusType {
        UNPROCESSABLE_ENTITY(422, "Unprocessable Entity");

        private final int code;
        private final String reason;
        private final Response.Status.Family family;

        Status(int statusCode, String reasonPhrase) {
            this.code = statusCode;
            this.reason = reasonPhrase;
            this.family = Response.Status.Family.familyOf(statusCode);
        }

        public Response.Status.Family getFamily() {
            return this.family;
        }

        public int getStatusCode() {
            return this.code;
        }

        public String getReasonPhrase() {
            return this.toString();
        }

        public String toString() {
            return this.reason;
        }
    }
}
