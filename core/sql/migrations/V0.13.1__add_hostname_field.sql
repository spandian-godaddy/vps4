DO $$ 
    BEGIN
        BEGIN
            ALTER TABLE  virtual_machine ADD COLUMN hostname VARCHAR(255);
        EXCEPTION
            WHEN duplicate_column THEN RAISE NOTICE 'column hostname already exists in virtual_machine.';
        END;
    END;
$$
