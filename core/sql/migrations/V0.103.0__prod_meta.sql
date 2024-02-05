CREATE TABLE IF NOT EXISTS prod_meta (
    entitlement_id UUID PRIMARY KEY NOT NULL,
    data_center INT REFERENCES data_center (data_center_id),
    product_id UUID,
    provision_date TIMESTAMP,
    fully_managed_email_sent BOOLEAN,
    purchased_at TIMESTAMP,
    released_at TIMESTAMP,
    relay_count INT
);