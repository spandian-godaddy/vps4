CREATE TABLE IF NOT EXISTS credit (
    credit_id UUID PRIMARY KEY NOT NULL,
    tier INT,
    managed_level INT,
    operating_system TEXT,
    control_panel TEXT,
    provision_date TIMESTAMP,
    shopper_id TEXT,
    monitoring INT,
    account_status INT,
    data_center INT,
    product_id UUID,
    fully_managed_email_sent BOOLEAN,
    reseller_id TEXT,
    pfid INT,
    purchased_at TIMESTAMP,
    customer_id UUID,
    expire_date TIMESTAMP,
    mssql TEXT,
    cdn_waf INT
);

DROP TABLE IF EXISTS prod_meta;

