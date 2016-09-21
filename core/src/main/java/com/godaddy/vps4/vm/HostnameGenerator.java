package com.godaddy.vps4.vm;

import java.util.UUID;

public class HostnameGenerator {
	
	public static String GetHostname() {
		
        return "vps-" + UUID.randomUUID().toString();

	}
}
