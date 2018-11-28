package com.godaddy.hfs.mailrelay;

public class MailRelay {
	
    public String ipv4Address;
    public int quota;
    public int relays;
    
    @Override
    public String toString(){
    	return String.format("MailRelayTarget [ip=%s, quota=%d]", ipv4Address, quota);
    }
}
