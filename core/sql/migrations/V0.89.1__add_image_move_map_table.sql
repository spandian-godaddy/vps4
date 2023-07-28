CREATE TABLE vm_move_image_map (
    id            SERIAL PRIMARY KEY,
    from_image_id BIGINT NOT NULL REFERENCES image (image_id),
    to_image_id   BIGINT NOT NULL REFERENCES image (image_id)
);

insert into image (hfs_name, control_panel_id, os_type_id, name, valid_on, valid_until, server_type_id, imported_image)
values ('openstack-ubuntu-1604',               0, 1, 'Ubuntu 16.04',               now_utc(), now_utc(), 3, 'true'),
       ('openstack-windows-2016',              0, 2, 'Windows 2016',               now_utc(), now_utc(), 3, 'true'),
       ('openstack-windows-2016-plesk-17',     2, 2, 'Windows 2016 (Plesk)',       now_utc(), now_utc(), 3, 'true'),
       ('openstack-ubuntu-1604-plesk-17',      2, 1, 'Ubuntu 16.04 (Plesk)',       now_utc(), now_utc(), 3, 'true'),
       ('openstack-debian-8',                  0, 1, 'Debian 8',                   now_utc(), now_utc(), 3, 'true'),
       ('openstack-ubuntu-1604-ispconfig-3',   0, 1, 'Ubuntu 16.04 (ISPConfig)',   now_utc(), now_utc(), 3, 'true'),
       ('openstack-windows-2016-plesk-18',     2, 2, 'Windows 2016 (Plesk)',       now_utc(), now_utc(), 3, 'true'),
       ('openstack-windows-2012r2',            0, 2, 'Windows 2012r2',             now_utc(), now_utc(), 3, 'true'),
       ('openstack-windows-2012r2-plesk-12.5', 2, 2, 'Windows 2012r2 (Plesk)',     now_utc(), now_utc(), 3, 'true'),
       ('openstack-hfs-windows-2012r2-plesk-17', 2, 2, 'Windows 2012r2 (Plesk)',   now_utc(), now_utc(), 3, 'true') ON CONFLICT DO NOTHING;

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'hfs-centos-7'and i2.hfs_name = 'hfs-centos7';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'hfs-centos-7-cpanel-11'and i2.hfs_name = 'hfs-centos7-cpanel';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'hfs-ubuntu-1604'and i2.hfs_name = 'hfs-ubuntu-1604';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'hfs-windows-2016'and i2.hfs_name = 'openstack-windows-2016';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'hfs-windows-2016-plesk-17'and i2.hfs_name = 'openstack-windows-2016-plesk-17';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'hfs-ubuntu-1604-plesk-17'and i2.hfs_name = 'openstack-ubuntu-1604-plesk-17';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'hfs-debian-8'and i2.hfs_name = 'openstack-debian-8';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'vps4-ubuntu-1604-ispconfig-3'and i2.hfs_name = 'openstack-ubuntu-1604-ispconfig-3';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'vps4-centos-7-plesk-17'and i2.hfs_name = 'hfs-centos7-plesk';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'vps4-centos-7-plesk-18'and i2.hfs_name = 'hfs-centos7-plesk';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'vps4-centos-7-cpanel-11'and i2.hfs_name = 'hfs-centos7-cpanel';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'vps4-windows-2016-plesk-18'and i2.hfs_name = 'openstack-windows-2016-plesk-18';

--These images don't appear to have any valid VMs, but the mappings were added for completeness
insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'centos-7'and i2.hfs_name = 'hfs-centos7';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'windows-2012r2'and i2.hfs_name = 'openstack-windows-2012r2';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'hfs-windows-2012r2'and i2.hfs_name = 'openstack-windows-2012r2';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'windows-2012r2-plesk-12.5'and i2.hfs_name = 'openstack-windows-2012r2-plesk-12.5';

insert into vm_move_image_map (from_image_id, to_image_id)
select i1.image_id, i2.image_id
from image i1, image i2
where i1.hfs_name = 'hfs-windows-2012r2-plesk-17'and i2.hfs_name = 'openstack-hfs-windows-2012r2-plesk-17';
