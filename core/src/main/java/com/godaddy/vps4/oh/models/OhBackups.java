package com.godaddy.vps4.oh.models;

import java.util.List;

/*
{
  "response": {
    "count": 6,
    "data": [
      {
        "uuid": "36535f08-bd18-41ec-a024-66580b19d114",
        "package_uuid": "3b1d5807-80e8-4ffa-b79b-cff31e2736c2",
        "job_uuid": "366061c9-bd18-11ec-8727-c43772d79070",
        "state": "complete",
        "tag": "Created by vzseo-noded",
        "type": "incremental",
        "purpose": "dr",
        "cdate": 1650067223.080955,
        "mdate": 1650067229.78679
      },
      ...
    ],
    "next": null
  },
  "status": "ok"
}
 */

public class OhBackups {
    public Response response;
    public String status;

    public static class Response {
        public int count;
        public List<OhBackup> data;
    }
}
