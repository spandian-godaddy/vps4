package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
{"data": {"Memory (kB)": [{"data": [503869, ...,
 505361.6, 504952.0, 504952.0], "name": "RAM used (kB) (kB)", "unit": "kB"}]},
 "end_time": "2019-07-23 16:04:24",
 "labels": [1563811464.0,...1563896664.0, 1563897264.0, 1563897864.0],
  "start_time": "2019-07-22 16:04:24",
  "timescale": "day",
   "url": "https://api2.panopta.com/v2/server/1105540/agent_resource/11483466"}
*/

public class PanoptaUsageGraph extends PanoptaGraph {
    @JsonProperty("labels")
    private void unwrapLabels(ArrayList<Long> labels) {
        this.timestamps = labels.stream().map(Instant::ofEpochSecond).collect(Collectors.toList());
    }

    @JsonProperty("data")
    private void unwrapData(ListWrapper wrapper) {
        this.values = wrapper.list.get(0).data;
    }

    private static class ListWrapper {
        private List<DataWrapper> list;

        @JsonAnySetter
        private void set(String ignored, List<DataWrapper> list) {
            this.list = list;
        }

        private static class DataWrapper {
            @JsonProperty("data")
            private List<Double> data;
        }
    }
}