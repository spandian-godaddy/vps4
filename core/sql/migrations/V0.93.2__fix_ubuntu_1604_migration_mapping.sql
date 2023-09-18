UPDATE vm_move_image_map SET to_image_id = (
    SELECT image_id from image WHERE hfs_name = 'openstack-ubuntu-1604'
) WHERE from_image_id = (
    SELECT image_id from image WHERE hfs_name = 'hfs-ubuntu-1604'
);
