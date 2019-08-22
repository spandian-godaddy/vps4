ALTER TABLE hfs_vm_tracking_record ADD COLUMN create_action_id INT REFERENCES vm_action (id);
ALTER TABLE hfs_vm_tracking_record ADD COLUMN cancel_action_id INT REFERENCES vm_action (id);
ALTER TABLE hfs_vm_tracking_record ADD COLUMN destroy_action_id INT REFERENCES vm_action (id);