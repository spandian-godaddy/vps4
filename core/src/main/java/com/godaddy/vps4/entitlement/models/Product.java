package com.godaddy.vps4.entitlement.models;

public class Product {
    public String uri;
    public String productFamily;
    public String productType;
    public String resourceType;
    public Term term;
    public String schemaVersion;
    public Boolean selfManagedOnly;
    public String[] agreements;
    public String instanceVersion;
    public Integer additionalMailRelay;
    public Integer additionalDedicatedIp;
    public Integer planTier;
    public String controlPanelType;
    public Integer managedLevel;
    public Boolean monitoring;
    public String operatingSystem;
    public String plan;
    public String mssql;
    @JsonAlias({"cdnwaf"}) public Integer cdnWaf;
    public Product() {
    }
}
