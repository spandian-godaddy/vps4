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
    @JsonProperty("service_type") public String serviceType;
    @JsonProperty("frequency") public long frequency;
    @JsonProperty("name_override") public String nameOverride;
    @JsonProperty("exclude_from_availability") public boolean excludeFromAvailability;
    @JsonProperty("outage_confirmation_delay") public long outageConfirmationDelay;
    @JsonProperty("port") public long port;
    @JsonProperty("server_interface") public String serverInterface;
    @JsonProperty("metadata") public Metadata metadata;

    public PanoptaApiNetworkServiceRequest(long serviceTypeId, long frequency, String nameOverride,
                                           boolean excludeFromAvailability, long outageConfirmationDelay, long port,
                                           String serverInterface, Metadata metadata) {
        this.serviceType = "https://api2.panopta.com/v2/network_service_type/" + serviceTypeId;
        this.frequency = frequency;
        this.nameOverride = nameOverride;
        this.excludeFromAvailability = excludeFromAvailability;
        this.outageConfirmationDelay = outageConfirmationDelay;
        this.port = port;
        this.serverInterface = serverInterface;
        this.metadata = metadata;
    }

    public static class Metadata {
        @JsonProperty("metric_override") public boolean metricOverride;

        public Metadata(boolean metricOverride) {
            this.metricOverride = metricOverride;
        }
    }

    public static class HttpsMetadata extends Metadata {
        @JsonProperty("http_ssl_expiration") public String httpSslExpiration;
        @JsonProperty("http_ssl_ignore") public String httpSslIgnore;

        public HttpsMetadata(boolean metricOverride, String httpSslExpiration, String httpSslIgnore) {
            super(metricOverride);
            this.httpSslExpiration = httpSslExpiration;
            this.httpSslIgnore = httpSslIgnore;
        }
    }
}
