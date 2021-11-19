package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
  "acknowledged": null,
  "compound_service": null,
  "compound_service_id": null,
  "compound_service_name": null,
  "description": "Ping: Incident simulation",
  "end_time": null,
  "exclude_from_availability": true,
  "has_active_maintenance": false,
  "hash": "QrV5m7aD22Ue3Is",
  "metadata": [],
  "network_service_type_list": [
    {
      "end_time": null,
      "exclude_from_availability": true,
      "metadata": "Incident simulation.",
      "network_service": "https://api2.panopta.com/v2/server/10571663/network_service/4916469",
      "service_type": "https://api2.panopta.com/v2/network_service_type/11",
      "start_time": "Fri, 05 Nov 2021 17:37:35 -0000",
      "status": "confirmed"
    }
  ],
  "next_action": null,
  "server": "https://api2.panopta.com/v2/server/10571663",
  "server_fqdn": "92.205.18.39",
  "server_group_id": "430037",
  "server_group_name": "Default Server Group",
  "server_id": "10571663",
  "server_name": "6dd97ae2-56a3-48af-bf93-9eaf7c622267",
  "severity": "critical",
  "start_time": "Fri, 05 Nov 2021 17:37:35 -0000",
  "status": "active",
  "summary": "",
  "tags": [],
  "type": "outage",
  "url": "https://api2.panopta.com/v2/outage/1853189891"
}
 */

public class PanoptaOutage {
    public long outageId;
    public Instant started;
    public Instant ended;
    public String reason;
    public Set<Long> metricIds = new HashSet<>();

    @JsonProperty("url")
    private void mapUrl(String url) {
        String suffix = url.substring(url.lastIndexOf('/') + 1);
        this.outageId = Long.parseLong(suffix);
    }

    @JsonProperty("start_time")
    private void mapStartTime(String start) {
        this.started = stringToInstant(start);
    }

    @JsonProperty("end_time")
    private void mapEndTime(String end) {
        this.ended = stringToInstant(end);
    }

    @JsonProperty("description")
    private void mapDescription(String description) {
        this.reason = description;
    }

    @JsonProperty("network_service_type_list")
    private void unwrapNetworkServiceList(Set<NetworkServiceType> networkServiceTypes) {
        Set<Long> networkServiceIds = networkServiceTypes.stream()
                                                         .map(t -> t.networkServiceId)
                                                         .collect(Collectors.toSet());
        this.metricIds.addAll(networkServiceIds);
    }

    @JsonProperty("server_resource")
    private void mapServerResource(String url) {
        String suffix = url.substring(url.lastIndexOf('/') + 1);
        this.metricIds.add(Long.parseLong(suffix));
    }

    private Instant stringToInstant(String time) {
        DateTimeFormatter dtf = DateTimeFormatter.RFC_1123_DATE_TIME;
        return (time == null) ? null : Instant.from(dtf.parse(time));
    }

    public static class NetworkServiceType {
        public long networkServiceId;

        @JsonProperty("network_service")
        private void mapNetworkService(String url) {
            String suffix = url.substring(url.lastIndexOf('/') + 1);
            this.networkServiceId = Long.parseLong(suffix);
        }
    }
}
