package com.godaddy.vps4.panopta;

import static com.godaddy.vps4.panopta.PanoptaGraph.Type.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
    @JsonProperty("network_service_list")
    private List<NetworkId> networkIdList = new ArrayList<>();

    public List<PanoptaGraphId> getList() {
        networkIdList.removeIf(e -> e.type == UNKNOWN);
        return new ArrayList<>(networkIdList);
    }

    public void setList(List<PanoptaGraphId> list) {
        this.networkIdList = new ArrayList<>();
        for (PanoptaGraphId id : list) {
            PanoptaNetworkIdList.NetworkId networkId = new PanoptaNetworkIdList.NetworkId();
            networkId.id = id.id;
            networkId.type = id.type;
            this.networkIdList.add(networkId);
        }
    }

    public static class NetworkId extends PanoptaGraphId {
        @JsonProperty("port")
        public void mapPort(int port) {
            switch (port) {
                case 21:
                    this.type = FTP;
                    break;
                case 22:
                    this.type = SSH;
                    break;
                case 25:
                    this.type = SMTP;
                    break;
                case 80:
                    this.type = HTTP;
                    break;
                case 110:
                    this.type = POP3;
                    break;
                case 143:
                    this.type = IMAP;
                    break;
                default:
                    this.type = UNKNOWN;
                    break;
            }
        }

        @JsonProperty("url")
        public void mapUrl(String url) {
            this.id = Integer.parseInt(url.split("/")[7]);
        }

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
