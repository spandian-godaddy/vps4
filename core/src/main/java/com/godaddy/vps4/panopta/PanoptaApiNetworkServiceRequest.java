package com.godaddy.vps4.panopta;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * {
 *   "service_type": null,
 *   "server_interface": null,
 *   "outage_confirmation_delay": null,
 *   "exclude_from_availability": null,
 *   "frequency": null
 *   "port": null
 *   "metadata": {
 *       "metric_override":null
 *    }
 * }
 */
public class PanoptaApiNetworkServiceRequest {
    private final String serviceTypeUrl;
    private final String serverInterface;
    private final long frequency;
    private final long outageConfirmationDelay;

    private final long port;
    private final boolean excludeFromAvailability;
    private final Metadata metadata;

    public PanoptaApiNetworkServiceRequest(long typeId, long value, boolean excludeFromAvailability,
                                           long outageConfirmationDelay, long port, String serverInterface,
                                           Metadata metadata) {
        this.serviceTypeUrl = "https://api2.panopta.com/v2/network_service_type/" + typeId;
        this.frequency = value;
        this.excludeFromAvailability = excludeFromAvailability;
        this.outageConfirmationDelay = outageConfirmationDelay;
        this.port = port;
        this.serverInterface = serverInterface;
        this.metadata = metadata;
    }

    public static class Metadata {
        boolean metricOverride;

        @JsonProperty("metric_override")
        public boolean getMetricOverride() {
            return metricOverride;
        }
    }

    public static class HttpsMetadata extends Metadata {
        String httpSslExpiration;
        String httpSslIgnore;

        @JsonProperty("http_ssl_expiration")
        public String getHttpSslExpiration() {
            return httpSslExpiration;
        }
        @JsonProperty("http_ssl_ignore")
        public String getHttpSslIgnore() {
            return httpSslIgnore;
        }
    }

    @JsonProperty("outage_confirmation_delay")
    public long getOutageConfirmationDelay() {
        return outageConfirmationDelay;
    }

    @JsonProperty("exclude_from_availability")
    public boolean getExcludeFromAvailability() {
        return excludeFromAvailability;
    }

    @JsonProperty("service_type")
    public String getServiceTypeUrl() {
        return serviceTypeUrl;
    }

    @JsonProperty("server_interface")
    public String getServerInterface() {
        return serverInterface;
    }

    @JsonProperty("frequency")
    public long getFrequency() {
        return frequency;
    }

    @JsonProperty("port")
    public long getPort() {
        return port;
    }

    @JsonProperty("metadata")
    public Metadata getMetadata() {
        return metadata;
    }
}
