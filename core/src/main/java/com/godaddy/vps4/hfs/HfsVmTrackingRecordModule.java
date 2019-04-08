package com.godaddy.vps4.hfs;

import com.godaddy.vps4.hfs.jdbc.JdbcHfsVmTrackingRecordService;
import com.google.inject.AbstractModule;

public class HfsVmTrackingRecordModule extends AbstractModule{

    @Override
    public void configure() {
        bind(HfsVmTrackingRecordService.class).to(JdbcHfsVmTrackingRecordService.class);
    }
}
