CREATE OR REPLACE PROCEDURE record_password_change(
    p_identity_id BIGINT,
    p_hashed_password TEXT,
    p_keep_history_size INTEGER DEFAULT 5
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
v_current_password_count INTEGER;
    v_max_history_size INTEGER := 20; -- Safety limit
BEGIN
    -- Validate inputs
    IF p_identity_id IS NULL THEN
        RAISE EXCEPTION 'Identity ID cannot be null';
END IF;

    IF p_hashed_password IS NULL OR LENGTH(TRIM(p_hashed_password)) = 0 THEN
        RAISE EXCEPTION 'Hashed password cannot be empty';
END IF;

    IF p_keep_history_size < 1 OR p_keep_history_size > v_max_history_size THEN
        RAISE EXCEPTION 'History size must be between 1 and %', v_max_history_size;
END IF;

    -- Check if password exists in history (prevent duplicates)
    PERFORM 1 FROM password_history
    WHERE identity_id = p_identity_id
    AND hashed_password = p_hashed_password
    LIMIT 1;

    IF FOUND THEN
        RAISE NOTICE 'Password already exists in history for identity %', p_identity_id;
        RETURN;
END IF;

    -- Insert new password record
INSERT INTO password_history (
    identity_id,
    hashed_password,
    created_at
) VALUES (
             p_identity_id,
             p_hashed_password,
             CURRENT_TIMESTAMP
         );

-- Get current count of passwords for this identity
SELECT COUNT(*) INTO v_current_password_count
FROM password_history
WHERE identity_id = p_identity_id;

-- Clean up old entries if we're over the limit
IF v_current_password_count > p_keep_history_size THEN
DELETE FROM password_history
WHERE id IN (
    SELECT id FROM (
                       SELECT
                           id,
                           ROW_NUMBER() OVER (ORDER BY created_at DESC) as row_num
                       FROM password_history
                       WHERE identity_id = p_identity_id
                   ) ranked
    WHERE row_num > p_keep_history_size
);

RAISE DEBUG 'Removed % old password entries for identity %',
            (v_current_password_count - p_keep_history_size),
            p_identity_id;
END IF;

    -- Update password_last_changed in identities table
UPDATE identities
SET
    password_last_changed = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = p_identity_id;

RAISE NOTICE 'Password changed recorded for identity %', p_identity_id;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'Error recording password change: %', SQLERRM;
END;
$$;