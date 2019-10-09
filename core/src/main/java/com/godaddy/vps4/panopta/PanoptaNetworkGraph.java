package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
    public void unwrapLabels(ArrayList<Long> labels) {
        this.timestamps = labels.stream().map(Instant::ofEpochSecond).collect(Collectors.toList());
    }

    @JsonProperty("data")
    @SuppressWarnings("unchecked")
    public void unwrapData(ArrayList<Object> data) {
        Map<String, Object> unwrappedMetrics = (Map<String, Object>) data.get(0);
        this.values = (List<Double>) unwrappedMetrics.get("data");
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}