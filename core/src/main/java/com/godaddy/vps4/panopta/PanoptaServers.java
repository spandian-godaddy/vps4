package com.godaddy.vps4.panopta;

import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
Representation of panopta response
{
  "meta": {
    "limit": 50,
    "next": null,
    "offset": 0,
    "previous": null,
    "total_count": 2
  },
  "server_list": [
    {
      "additional_fqdns": [
        "169.254.254.28",
        "64.202.187.12"
      ],
      "agent_heartbeat_delay": 10,
      "agent_heartbeat_enabled": true,
      "agent_heartbeat_notification_schedule": "https://api2.panopta.com/v2/notification_schedule/-1",
      "agent_installed": true,
      "agent_last_sync_time": "2019-07-30 21:44:57",
      "agent_version": "19.12.5",
      "attributes": [
        {
          "server_attribute_type": "https://api2.panopta.com/v2/server_attribute_type/315",
          "url": "https://api2.panopta.com/v2/server/1105606/server_attribute/803531",
          "value": "agent"
        },
        {
          "server_attribute_type": "https://api2.panopta.com/v2/server_attribute_type/290",
          "url": "https://api2.panopta.com/v2/server/1105606/server_attribute/803532",
          "value": "Windows"
        },
        {
          "server_attribute_type": "https://api2.panopta.com/v2/server_attribute_type/294",
          "url": "https://api2.panopta.com/v2/server/1105606/server_attribute/803533",
          "value": "1"
        },
        {
          "server_attribute_type": "https://api2.panopta.com/v2/server_attribute_type/293",
          "url": "https://api2.panopta.com/v2/server/1105606/server_attribute/803534",
          "value": "Microsoft Windows NT 6.2.9200.0"
        },
        {
          "server_attribute_type": "https://api2.panopta.com/v2/server_attribute_type/295",
          "url": "https://api2.panopta.com/v2/server/1105606/server_attribute/803535",
          "value": "x64"
        }
      ],
      "auxiliary_notification": {
        "agent_heartbeats": [],
        "agent_thresholds": [],
        "network_outages": [],
        "snmp_heartbeats": [],
        "snmp_thresholds": [],
        "wmi_heartbeats": [],
        "wmi_thresholds": []
      },
      "auxiliary_notification_schedules": [],
      "billing_type": "advanced",
      "countermeasures_enabled": false,
      "created": "Fri, 24 May 2019 23:22:29 -0000",
      "current_outages": [],
      "current_state": "up",
      "deleted": null,
      "description": "",
      "device_type": "server",
      "fqdn": "s64-202-187-12",
      "name": "s64-202-187-12",
      "notification_schedule": "https://api2.panopta.com/v2/notification_schedule/184642",
      "notify_agent_heartbeat_failure": true,
      "parent_server": null,
      "partner_server_id": null,
      "primary_monitoring_node": "https://api2.panopta.com/v2/monitoring_node/51",
      "server_group": "https://api2.panopta.com/v2/server_group/346861",
      "server_key": "d3cn-thrm-xovb-ona8",
      "server_template": [],
      "snmp_heartbeat_delay": 10,
      "snmp_heartbeat_enabled": false,
      "snmp_heartbeat_notification_schedule": null,
      "snmpcredential": null,
      "status": "active",
      "tags": [],
      "template_ignore_agent_heartbeat": false,
      "template_ignore_snmp_heartbeat": false,
      "url": "https://api2.panopta.com/v2/server/1105606"
    }
  ]
}
 */
public class PanoptaServers {
    public Meta meta;

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public static class Meta {
        public int limit;
        public int offset;
        public String next;
        public String previous;

        public Meta() {}

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    @JsonProperty("server_list")
    public List<Server> servers;

    public List<Server> getServers() {
        return servers;
    }

    public static class Server {
        @JsonProperty("agent_last_sync_time") public String agentLastSynced;
        public String created;
        public String deleted;
        public String description;
        public String fqdn;
        public String name;
        @JsonProperty("server_group") public String serverGroup;
        @JsonProperty("server_key") public String serverKey;
        public String status;
        public List<String> tags;
        public String url;

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
