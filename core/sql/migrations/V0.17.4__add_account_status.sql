CREATE TABLE account_status (
    account_status_id INT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

INSERT INTO account_status (account_status_id, name) VALUES (1, 'active'), (2, 'suspended'), (3, 'abuse_suspended'), (4, 'removed');

ALTER TABLE virtual_machine ADD COLUMN account_status_id INT REFERENCES account_status(account_status_id) NOT NULL DEFAULT 1;