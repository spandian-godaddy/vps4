CREATE OR REPLACE FUNCTION get_or_create_user(p_shopper_id TEXT)

RETURNS vps4_user
AS $$
DECLARE
    t_user vps4_user;
BEGIN
    
    SELECT * FROM vps4_user INTO t_user WHERE shopper_id=p_shopper_id;
    
    IF NOT FOUND THEN
        BEGIN
            INSERT INTO vps4_user (shopper_id) VALUES (p_shopper_id)
              RETURNING * INTO t_user;
        EXCEPTION WHEN unique_violation THEN
            -- user was created between our SELECT and INSERT
            SELECT * FROM vps4_user INTO t_user WHERE shopper_id=p_shopper_id;
            
            -- there is a small chance that (for whatever reason)
            -- the user is deleted after the lazy creation (the INSERT
            --  executed by the other pg connection)
            -- before we're able to select it, but that would seem to
            -- be the pathological case, since the user should be stable/
            -- immutable after lazy creation
        END;
    END IF;
    
    RETURN t_user;

END;
$$ LANGUAGE plpgsql;