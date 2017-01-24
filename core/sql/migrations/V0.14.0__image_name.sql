
ALTER TABLE image
    RENAME COLUMN name TO hfs_name;
    
ALTER TABLE image
    ADD COLUMN name TEXT,
    ADD COLUMN valid_on TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN valid_until TIMESTAMP NOT NULL DEFAULT 'infinity';

UPDATE image SET name='CentOS 7' WHERE hfs_name='centos-7';
UPDATE image SET name='Windows 2012 R2' WHERE hfs_name='windows-2012r2';
UPDATE image SET name='Centos 7 (cPanel 11)' WHERE hfs_name='centos-7-cPanel-11';

ALTER TABLE image ALTER COLUMN name SET NOT NULL;
