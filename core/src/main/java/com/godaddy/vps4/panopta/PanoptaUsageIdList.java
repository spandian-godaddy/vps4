package com.godaddy.vps4.panopta;

import static com.godaddy.vps4.vm.VmMetric.CPU;
import static com.godaddy.vps4.vm.VmMetric.DISK;
import static com.godaddy.vps4.vm.VmMetric.RAM;
import static com.godaddy.vps4.vm.VmMetric.UNKNOWN;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
  "agent_resource_list": [
    {
      "agent_resource_type": "https://api2.panopta.com/v2/agent_resource_type/675",
      "formatted_name": "Memory: RAM percent usage",
      "frequency": 60,
      "name": "Memory: RAM percent usage",
      "name_override": "",
      "plugin_textkey": "memory",
      "resource_option": {},
      "resource_textkey": "ram.percent",
      "status": "active",
      "tags": [],
      "template": "https://api2.panopta.com/v2/server_template/1146058",
      "template_agent_resource": "https://api2.panopta.com/v2/server/1146058/agent_resource/12994971",
      "thresholds": [],
      "url": "https://api2.panopta.com/v2/server/1151747/agent_resource/13321688"
    },[...]
  ]
}
*/

public class PanoptaUsageIdList {
    @JsonIgnore public List<PanoptaGraphId> value;

    @JsonProperty("agent_resource_list")
    private void unwrapResourceList(ArrayList<PanoptaUsageId> list) {
        list.removeIf(e -> e.type == UNKNOWN);
        this.value = new ArrayList<>(list);
    }

    public static class PanoptaUsageId extends PanoptaGraphId {
        @JsonProperty("name")
        public void mapName(String key) {
            switch (key) {
                case "vps4_cpu_total_percent_used":
                    this.type = CPU;
                    break;
                case "vps4_disk_total_percent_used":
                    this.type = DISK;
                    break;
                case "vps4_ram_total_percent_used":
                    this.type = RAM;
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

        @JsonProperty("resource_option")
        public void mapResourceOption(Object option) {
            if (option instanceof String) {
                Pattern pattern = Pattern.compile("^.+ mounted at (.+)$");
                Matcher matcher = pattern.matcher((String) option);
                if (matcher.find()) {
                    this.metadata.put("mountPoint", matcher.group(1));
                }
            }
        }
    }
}