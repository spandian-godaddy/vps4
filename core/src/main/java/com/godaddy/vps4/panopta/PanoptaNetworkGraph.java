package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
  "data": [
    {
      "data": [
        null,
        316.75004959106445,
        316.75004959106445,
        ...
      ],
      "monitor_point": "FTP Port - 148.66.134.27",
      "name": "FTP Port - 148.66.134.27",
      "unit": "ms"
    }
  ],
  "end_time": "2019-09-13 02:07:28",
  "labels": [
    1568336848,
    1568336908,
    ...
  ],
  "start_time": "2019-09-13 01:07:28",
  "timescale": "hour",
  "url": "https://api2.panopta.com/v2/server/1168817/network_service/2796844"
}
*/

public class PanoptaNetworkGraph extends PanoptaGraph {
    @JsonProperty("labels")
    private void unwrapLabels(ArrayList<Long> labels) {
        this.timestamps = labels.stream().map(Instant::ofEpochSecond).collect(Collectors.toList());
    }

    @JsonProperty("data")
    private void unwrapData(List<DataWrapper> list) {
        this.values = list.get(0).data;
    }

    private static class DataWrapper {
        @JsonProperty("data")
        private List<Double> data;
    }
}