-- ============================================================
-- PRODUCTION RESET SCRIPT
-- Clears all data and seeds:
--   Company : TS Construction
--   Admin   : Agilan K S  (agilanksds@gmail.com / agilan@123)
-- ============================================================

-- 1. Disable FK constraints temporarily
SET session_replication_role = replica;

-- 2. Truncate ALL tables (order doesn't matter with FK disabled)
TRUNCATE TABLE
    audit_log,
    expense_item,
    expense,
    payment,
    client_payment,
    sub_category,
    master_category,
    vendor,
    project,
    app_user,
    company
RESTART IDENTITY CASCADE;

-- 3. Re-enable FK constraints
SET session_replication_role = DEFAULT;

-- 4. Insert company
INSERT INTO company (name, address, phone, email, created_at)
VALUES ('TS Construction', NULL, NULL, NULL, NOW());

-- 5. Insert admin user (BCrypt hash of 'agilan@123')
INSERT INTO app_user (
    company_id,
    email,
    name,
    password,
    role,
    first_name,
    last_name,
    date_joined,
    is_active,
    is_staff,
    is_superuser
)
VALUES (
    1,                                                        -- company_id (TS Construction)
    'agilanksds@gmail.com',
    'Agilan K S',
    '$2b$12$2zV74fLQZFWMPYK.MEW7ruqVWgkZ0xvCiMJThRNfDNLfVKSZH9yYG',  -- agilan@123
    'ADMIN',
    'Agilan',
    'K S',
    NOW(),
    true,
    true,
    true
);

-- 6. Verify
SELECT 'Company:' AS label, company_id, name FROM company;
SELECT 'User:'    AS label, user_id, name, email, role FROM app_user;
