-- Read-only queries for checking the New Life Hospital database (pgAdmin Query Tool or psql).
-- Connect to database "newlife_hospital".

-- 1. Row count of every table
SELECT 'users' AS table_name, COUNT(*) FROM users
UNION ALL SELECT 'staff_profiles', COUNT(*) FROM staff_profiles
UNION ALL SELECT 'field_validations', COUNT(*) FROM field_validations
UNION ALL SELECT 'doctors', COUNT(*) FROM doctors
UNION ALL SELECT 'hospital_services', COUNT(*) FROM hospital_services
UNION ALL SELECT 'patients', COUNT(*) FROM patients
UNION ALL SELECT 'invoices', COUNT(*) FROM invoices
UNION ALL SELECT 'invoice_items', COUNT(*) FROM invoice_items
UNION ALL SELECT 'cms_content', COUNT(*) FROM cms_content;

-- 2. Login accounts (passwords are BCrypt hashes and cannot be read back)
SELECT u.id, u.username, u.role, u.enabled, sp.full_name, sp.email, sp.phone_number, u.created_at
FROM users u LEFT JOIN staff_profiles sp ON sp.user_id = u.id
ORDER BY u.role, u.username;

-- 3. Doctors
SELECT id, name, specialization, qualification, experience_years, is_active, photo_url
FROM doctors ORDER BY name;

-- 4. Charges sheet
SELECT id, category, service_name, cost, is_active
FROM hospital_services ORDER BY category, service_name;

-- 5. Website text (CMS)
SELECT section_key, label, LEFT(content_text, 80) AS preview, updated_by, updated_at
FROM cms_content ORDER BY id;

-- 6. Validation rules for one form (change 'patient' to staff / doctor / service / invoice / cms / login)
SELECT field_name, field_label, data_type, required, regex_pattern, min_length, max_length, error_message
FROM field_validations WHERE form_name = 'patient' ORDER BY id;

-- 7. Patients (the ID number column is encrypted, so only the last 4 digits are readable)
SELECT patient_code, name, id_type, 'XXXX-' || id_number_last4 AS id_number,
       dob, DATE_PART('year', AGE(dob)) AS age, gender, contact_number, address, created_by, created_at
FROM patients ORDER BY created_at DESC;

-- 8. Invoices with patient, doctor and staff
SELECT i.invoice_number, i.invoice_date, p.patient_code, p.name AS patient, d.name AS doctor, i.staff_name,
       i.total_amount, i.discount, i.net_amount, i.paid_amount, i.balance_amount, i.payment_status
FROM invoices i
JOIN patients p ON p.id = i.patient_id
LEFT JOIN doctors d ON d.id = i.doctor_id
ORDER BY i.created_at DESC;

-- 9. Line items of one invoice (replace the invoice number)
SELECT ii.service_name, ii.unit_price, ii.quantity, ii.amount
FROM invoice_items ii JOIN invoices i ON i.id = ii.invoice_id
WHERE i.invoice_number = 'NLH-INV-20261018-00001'
ORDER BY ii.id;

-- 10. Revenue per day (same numbers as the Admin dashboard)
SELECT invoice_date, COUNT(*) AS invoices, SUM(net_amount) AS billed, SUM(paid_amount) AS collected,
       SUM(balance_amount) AS outstanding
FROM invoices GROUP BY invoice_date ORDER BY invoice_date DESC;

-- 11. Revenue per month
SELECT TO_CHAR(invoice_date, 'YYYY-MM') AS month, COUNT(*) AS invoices, SUM(net_amount) AS billed,
       SUM(paid_amount) AS collected
FROM invoices GROUP BY 1 ORDER BY 1 DESC;

-- 12. Patients with unpaid balances
SELECT p.patient_code, p.name, p.contact_number, i.invoice_number, i.balance_amount
FROM invoices i JOIN patients p ON p.id = i.patient_id
WHERE i.balance_amount > 0 ORDER BY i.balance_amount DESC;

-- 13. Which migration scripts have run
SELECT installed_rank, version, description, installed_on, success
FROM flyway_schema_history ORDER BY installed_rank;
