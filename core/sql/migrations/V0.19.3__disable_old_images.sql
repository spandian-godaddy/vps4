UPDATE image 
    SET hfs_name = 'hfs-windows-2012r2-plesk-17' 
    WHERE hfs_name = 'hfs-windows-2012r2-plesk-12.5';

UPDATE image
    SET valid_until = NOW()
    where image_id in (1,2,3,4);