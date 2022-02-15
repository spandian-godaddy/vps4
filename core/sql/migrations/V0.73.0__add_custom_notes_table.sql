CREATE TABLE vm_custom_notes (
    id                       SERIAL    PRIMARY KEY
    , vm_id                  UUID      REFERENCES virtual_machine(vm_id)
    , created               TIMESTAMP   NOT NULL    DEFAULT now_utc()
    , author           TEXT
    , note                   TEXT
);

