package com.godaddy.vps4.panopta;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
  "meta": {
    "limit": 10,
    "next": null,
    "offset": 0,
    "order": "desc",
    "order_by": "start_time",
    "previous": null,
    "total_count": 2
  },
  "outage_list": [
    {
      "acknowledged": null,
      "compound_service": null,
      "compound_service_id": null,
      "compound_service_name": null,
      "description": "Agent Heartbeat",
      "end_time": "Fri, 23 Aug 2019 22:50:18 -0000",
      "exclude_from_availability": false,
      "has_active_maintenance": false,
      "hash": "NLcGUUvjHKCrfBy",
      "network_service_type_list": [
        {
          "end_time": "Fri, 23 Aug 2019 22:50:18 -0000",
          "exclude_from_availability": false,
          "metadata": "",
          "network_service": "https://api2.panopta.com/v2/server/1168817/network_service/2819243",
          "service_type": "https://api2.panopta.com/v2/network_service_type/1332",
          "start_time": "Fri, 23 Aug 2019 22:31:49 -0000",
          "status": "resolved"
        },
        ...
      ],
      "next_action": null,
      "server": "https://api2.panopta.com/v2/server/1168817",
      "server_fqdn": "148.66.134.27",
      "server_group_id": "349024",
      "server_group_name": "INCOMING SERVERS",
      "server_id": "1168817",
      "server_name": "s148-66-134-27.secureserver.net",
      "severity": "critical",
      "start_time": "Fri, 23 Aug 2019 22:31:49 -0000",
      "status": "resolved",
      "type": "outage",
      "url": "https://api2.panopta.com/v2/outage/1727791121"
    },
    ...
  ]
}
*/

public class PanoptaOutage {
    @JsonProperty("outage_list")
    public List<Outage> outageList;
    public Meta meta;

    public static class Outage {
        public String status;
        public String description;
        @JsonProperty("start_time")
        public String startTime;
        @JsonProperty("end_time")
        public String endTime;

        public static class Service {
            public String name;
            public String details;
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class Meta {
        public int limit;
        public int offset;
        public String next;
        public String previous;
        @JsonProperty("total_count")
        public int totalCount;

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}