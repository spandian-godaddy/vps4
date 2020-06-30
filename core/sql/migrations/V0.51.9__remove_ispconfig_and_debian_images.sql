UPDATE image SET valid_until = now_utc() WHERE hfs_name IN (
    'debian8_64',
    'hfs-debian-8',
    'vps4-ubuntu-1604-ispconfig-3'
);