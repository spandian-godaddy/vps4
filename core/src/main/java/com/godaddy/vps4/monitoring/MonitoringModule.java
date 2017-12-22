package com.godaddy.vps4.monitoring;

import com.godaddy.vps4.monitoring.iris.IrisMonitoringNotificationService;
import com.godaddy.vps4.monitoring.iris.irisClient.IrisWebService;
import com.godaddy.vps4.monitoring.iris.irisClient.IrisWebServiceSoap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class MonitoringModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MonitoringNotificationService.class).to(IrisMonitoringNotificationService.class);
    }

    @Provides
    public static IrisWebServiceSoap provideIrisWebServiceSoap() {
        IrisWebService irisWebService = new IrisWebService();
        return irisWebService.getIrisWebServiceSoap12();
    }
}
