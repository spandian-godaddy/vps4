package com.godaddy.vps4.panopta;

import java.util.List;

public class PanoptaGraph {
    public enum Type {UNKNOWN, CPU, RAM, DISK, FTP, SSH, SMTP, HTTP, IMAP, POP3}

    public Type type;
    public List<Long> labels;
    public List<Double> values;
}