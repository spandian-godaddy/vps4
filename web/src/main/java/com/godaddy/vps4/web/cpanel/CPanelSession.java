package com.godaddy.vps4.web.cpanel;

import java.time.Instant;

public class CPanelSession {
    public class MetaData {
        public int result;
        public String reason;
        public String command;
        public int version;
    }

    public class Data {
        public String cpSecurityToken;
        public String service;
        private Instant expires;
        public String session;
        public String url;

        public Instant getExpires() {
            return expires;
        }

        public void setExpires(int epochSeconds) {
            expires = Instant.ofEpochSecond(epochSeconds);
        }
    }

    public Data data;
    public MetaData metadata;
}
