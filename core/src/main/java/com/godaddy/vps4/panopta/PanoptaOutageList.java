package com.godaddy.vps4.panopta;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
  "meta": { ... },
  "outage_list": [
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
  ]
}
 */

public class PanoptaOutageList {
    @JsonIgnore
    public List<PanoptaOutage> value;

    @JsonProperty("outage_list")
    private void unwrapOutageList(List<PanoptaOutage> value) {
        this.value = value;
    }
}
