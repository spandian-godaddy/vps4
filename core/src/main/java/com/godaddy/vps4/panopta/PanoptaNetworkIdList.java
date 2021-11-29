package com.godaddy.vps4.panopta;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
  "network_service_list": [
    {
      "cascade_override": false,
      "exclude_from_availability": false,
      "exclude_response_time_from_availability": false,
      "frequency": 60,
      "ip_type": "v4",
      "metadata": {
        "plugin_textkey": [],
        "resource_textkey": []
      },
      "monitor_node": "https://api2.panopta.com/v2/monitoring_node/51",
      "name": "",
      "name_override": "",
      "notification_schedule": null,
      "outage_confirmation_delay": 0,
      "port": 22,
      "response_time_delay": null,
      "response_time_threshold": null,
      "server_interface": "148.66.134.27",
      "service_type": "https://api2.panopta.com/v2/network_service_type/311",
      "status": "active",
      "tags": [],
      "template": "https://api2.panopta.com/v2/server_template/1146058",
      "template_network_service_resource": "https://api2.panopta.com/v2/server/1146058/network_service/2740665",
      "url": "https://api2.panopta.com/v2/server/1168817/network_service/2796843"
    },[...]
  ]
}
*/

public class PanoptaNetworkIdList {
    @JsonIgnore public List<PanoptaMetricId> value;

    @JsonProperty("network_service_list")
    private void unwrapServiceList(ArrayList<PanoptaNetworkId> list) {
        this.value = new ArrayList<>(list);
    }

    public static class PanoptaNetworkId extends PanoptaMetricId {
        @JsonProperty("service_type")
        private void mapType(String url) {
            this.typeId = Long.parseLong(url.substring(url.lastIndexOf('/') + 1));
        }

        @JsonProperty("url")
        private void mapUrl(String url) {
            this.id = Integer.parseInt(url.split("/")[7]);
        }

        @JsonProperty("server_interface")
        private void mapServerInterface(String serverInterface) {
            this.serverInterface = serverInterface;
        }
    }
}
