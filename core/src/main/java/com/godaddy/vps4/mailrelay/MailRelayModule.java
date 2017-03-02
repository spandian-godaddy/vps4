package com.godaddy.vps4.mailrelay;

import com.google.inject.AbstractModule;

public class MailRelayModule extends AbstractModule{

    @Override
    public void configure() {
        bind(MailRelayService.class);
    }
}
