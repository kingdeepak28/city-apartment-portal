-- Default Super Admin account. Change this password immediately after first login.
-- Login: super.admin@societyportal.local / Admin@123
INSERT INTO admin_users (id, name, email, mobile, password_hash, role, status)
VALUES (
    gen_random_uuid(),
    'Super Admin',
    'super.admin@societyportal.local',
    '9999999999',
    '$2b$10$expQT7qUuCUb/Tv8S0O2j.ZmnGOZ503bQAmFj7/l/TN7KbIgxFOT.',
    'SUPER_ADMIN',
    'ACTIVE'
);

-- A sample approved member for local testing/demo.
-- Login: demo.member@societyportal.local / Member@123
INSERT INTO users (id, name, email, mobile, password_hash, flat_no, block, resident_type,
                    status, email_verified, mobile_verified, approved_on)
VALUES (
    gen_random_uuid(),
    'Demo Member',
    'demo.member@societyportal.local',
    '9000000001',
    '$2b$10$Out3QiXncF6XDId5YJJDeO9JmWEkYwd1qqfiwn8QsCAlKCnktn/se',
    'A-101',
    'A',
    'OWNER',
    'ACTIVE',
    TRUE,
    TRUE,
    now()
);

-- Report categories (seeded examples from the spec)
INSERT INTO categories (type, name, display_order) VALUES
    ('REPORT', 'Annual Report', 1),
    ('REPORT', 'Audited Financial Statement', 2),
    ('REPORT', 'Income & Expenditure', 3),
    ('REPORT', 'Budget', 4),
    ('REPORT', 'Audit Report', 5),
    ('REPORT', 'Maintenance Report', 6),
    ('REPORT', 'Water Testing Report', 7),
    ('REPORT', 'Fire Safety / NOC Report', 8),
    ('REPORT', 'Lift AMC Report', 9),
    ('REPORT', 'Security Report', 10),
    ('REPORT', 'Housekeeping Report', 11),
    ('REPORT', 'Sub-committee Report', 12),
    ('REPORT', 'Legal Report', 13),
    ('REPORT', 'Statutory Compliance', 14);

-- Notice categories
INSERT INTO categories (type, name, display_order) VALUES
    ('NOTICE', 'General', 1),
    ('NOTICE', 'Maintenance', 2),
    ('NOTICE', 'Water & Power', 3),
    ('NOTICE', 'Security', 4),
    ('NOTICE', 'Event', 5),
    ('NOTICE', 'Payment', 6),
    ('NOTICE', 'Statutory', 7),
    ('NOTICE', 'Emergency', 8);

-- Photo album categories (phase 2 scaffold)
INSERT INTO categories (type, name, display_order) VALUES
    ('PHOTO', 'Festival', 1),
    ('PHOTO', 'Event', 2),
    ('PHOTO', 'Facility', 3),
    ('PHOTO', 'Construction & Maintenance', 4),
    ('PHOTO', 'Meeting', 5),
    ('PHOTO', 'General', 6);

-- Meeting types (phase 2 scaffold)
INSERT INTO categories (type, name, display_order) VALUES
    ('MEETING', 'AGM', 1),
    ('MEETING', 'EGM', 2),
    ('MEETING', 'Managing Committee', 3),
    ('MEETING', 'Sub-committee', 4),
    ('MEETING', 'Special', 5);

-- System settings
INSERT INTO settings (key, value, description) VALUES
    ('society.name', 'Green Valley Co-operative Housing Society', 'Society display name'),
    ('society.regNo', '', 'Society registration number'),
    ('society.address', '', 'Society registered address'),
    ('society.contactEmail', 'office@societyportal.local', 'Society contact email'),
    ('society.contactPhone', '', 'Society contact phone'),
    ('file.allowedExtensions', 'pdf,doc,docx,xls,xlsx,ppt,pptx,jpg,jpeg,png,zip', 'Allowed upload extensions'),
    ('file.maxSizeMb', '25', 'Maximum upload size in MB'),
    ('notice.numberFormat', 'NOT/{FY}/{SEQ}', 'Notice number format'),
    ('tender.numberFormat', 'TND/{FY}/{SEQ}', 'Tender number format'),
    ('approval.slaDays', '3', 'Days before a pending approval is flagged overdue');
