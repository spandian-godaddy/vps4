DO $$ 
    BEGIN
        BEGIN
            ALTER TABLE  credit ADD COLUMN monitoring INT NOT NULL DEFAULT 0;
        EXCEPTION
            WHEN duplicate_column THEN RAISE NOTICE 'column monitoring already exists in credit.';
        END;
    END;
$$
