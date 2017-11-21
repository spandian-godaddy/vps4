CREATE TABLE scheduled_job_type (
	id INT PRIMARY KEY,
	job_group VARCHAR(255)
);

INSERT INTO scheduled_job_type (id, job_group) VALUES
	(1, 'backups'),
	(2, 'zombie');

CREATE TABLE scheduled_job (
	id UUID PRIMARY KEY,
	vm_id UUID REFERENCES virtual_machine (vm_id),
	scheduled_job_type_id INT REFERENCES scheduled_job_type(id),
	created TIMESTAMP DEFAULT now_utc()
);

ALTER TABLE virtual_machine ADD COLUMN canceled TIMESTAMP DEFAULT 'infinity';
UPDATE virtual_machine SET canceled = valid_until;