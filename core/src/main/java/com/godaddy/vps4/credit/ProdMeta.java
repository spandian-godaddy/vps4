package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.UUID;

import com.godaddy.vps4.vm.DataCenter;

public class ProdMeta {

    public DataCenter dataCenter;
    public UUID productId;
    public Instant provisionDate;
    public boolean fullyManagedEmailSent;
    public boolean planChangePending;
    public Instant purchasedAt;
    public boolean suspended;
}

/*
Prod Meta Fields:
DATA_CENTER,
PRODUCT_ID,
PROVISION_DATE,
FULLY_MANAGED_EMAIL_SENT,
PLAN_CHANGE_PENDING,
PURCHASED_AT,
RELEASED_AT,
RELAY_COUNT,
SUSPENDED,
BILLING_SUSPENDED_FLAG; 
*/