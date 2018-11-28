package com.godaddy.hfs.mailrelay;

public class MailRelayHistory {

	public String date;
    public int relays;
    public int quota;
    
    @Override
    public String toString(){
    	return String.format("MailRelayHistory [date=%s, relays=%d, quota=%d", date, relays, quota);
    }   
}
