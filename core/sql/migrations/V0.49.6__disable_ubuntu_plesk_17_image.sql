-- Disable ubuntu-1604 plesk-17 image
UPDATE image SET valid_until = now_utc() WHERE hfs_name='hfs-ubuntu-1604-plesk-17';

