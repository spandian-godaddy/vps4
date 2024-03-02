package com.godaddy.vps4.web.credit;

import java.time.Instant;
import java.util.UUID;

import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;

public class Vps4Credit {

    public Vps4Credit() {
    }

    public Vps4Credit (VirtualMachineCredit virtualMachineCredit, DataCenterService dataCenterService) {
        if(virtualMachineCredit == null) {
            return;
        }
        orionGuid = virtualMachineCredit.entitlementData.entitlementId;
        tier = virtualMachineCredit.entitlementData.tier;
        managedLevel = virtualMachineCredit.entitlementData.managedLevel;
        operatingSystem = virtualMachineCredit.entitlementData.operatingSystem;
        controlPanel = virtualMachineCredit.entitlementData.controlPanel;
        provisionDate = virtualMachineCredit.prodMeta.provisionDate;
        shopperId = virtualMachineCredit.shopperId;
        monitoring = virtualMachineCredit.entitlementData.monitoring;
        accountStatus = virtualMachineCredit.entitlementData.accountStatus;
        dataCenter = dataCenterService.getDataCenter(virtualMachineCredit.prodMeta.dataCenter);
        productId = virtualMachineCredit.prodMeta.productId;
        fullyManagedEmailSent = virtualMachineCredit.prodMeta.fullyManagedEmailSent;
        resellerId = virtualMachineCredit.resellerId;
        pfid = virtualMachineCredit.entitlementData.pfid;
        purchasedAt = virtualMachineCredit.prodMeta.purchasedAt;
        suspended = false;
        customerId = virtualMachineCredit.entitlementData.customerId;
        expireDate = virtualMachineCredit.entitlementData.expireDate;
        autoRenew = false;
        mssql = virtualMachineCredit.mssql;
        cdnWaf = virtualMachineCredit.getCdnWaf();
        ded4 = virtualMachineCredit.isDed4();
        hasMonitoring = virtualMachineCredit.hasMonitoring();
        managed = virtualMachineCredit.isManaged();
        vmSuspended = false;
        planChangePending = false;
    }
    
    public UUID orionGuid;
    public int tier;
    public int managedLevel;
    public String operatingSystem;
    public String controlPanel;
    public Instant provisionDate;
    public String shopperId;
    public int monitoring;
    public AccountStatus accountStatus;
    public DataCenter dataCenter;
    public UUID productId;
    public boolean fullyManagedEmailSent;
    public String resellerId;
    public int pfid;
    public Instant purchasedAt;
    public boolean suspended;
    public UUID customerId;
    public Instant expireDate;
    public boolean autoRenew;
    public String mssql;
    public int cdnWaf;
    public boolean ded4;
    public boolean hasMonitoring;
    public boolean managed;
    public boolean vmSuspended;
    public boolean planChangePending;
}
