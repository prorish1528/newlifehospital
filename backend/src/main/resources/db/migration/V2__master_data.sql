-- Master data: validation rules, starter charges sheet, starter doctors and CMS text.
-- Doctors, prices and contact details are illustrative placeholders; replace them through the Admin portal.

-- ---------------------------------------------------------------------------
-- Field validation rules (single source of truth for client and server)
-- Rules named idNumber_<ID_TYPE> override the generic idNumber rule for that ID type.
-- ---------------------------------------------------------------------------
INSERT INTO field_validations (form_name, field_name, field_label, data_type, required, regex_pattern, min_length, max_length, error_message) VALUES
('login',   'username',          'Username',            'STRING',  TRUE,  NULL, 1, 30,  'Username is required.'),
('login',   'password',          'Password',            'STRING',  TRUE,  NULL, 1, 64,  'Password is required.'),

('patient', 'name',              'Full Name',           'STRING',  TRUE,  '^[A-Za-z][A-Za-z .''-]*$', 2, 100, 'Name must be 2-100 letters and may contain spaces, dots, hyphens or apostrophes.'),
('patient', 'idType',            'ID Type',             'STRING',  TRUE,  '^(AADHAAR|PASSPORT|VOTER_ID|DRIVING_LICENSE|OTHER)$', NULL, 20, 'Please select a valid ID type.'),
('patient', 'idTypeOther',       'Other ID Type',       'STRING',  FALSE, '^[A-Za-z0-9 ./-]+$', 2, 50, 'Describe the ID type in 2-50 characters (letters, digits, space . / -).'),
('patient', 'idNumber',          'ID Number',           'STRING',  TRUE,  '^[A-Z0-9/]+$', 4, 20, 'ID number must be 4-20 letters or digits.'),
('patient', 'idNumber_AADHAAR',  'Aadhaar Number',      'STRING',  TRUE,  '^[2-9][0-9]{11}$', 12, 12, 'Aadhaar number must be 12 digits and cannot start with 0 or 1.'),
('patient', 'idNumber_PASSPORT', 'Passport Number',     'STRING',  TRUE,  '^[A-Z][0-9]{7}$', 8, 8, 'Passport number must be 1 letter followed by 7 digits (e.g. K1234567).'),
('patient', 'idNumber_VOTER_ID', 'Voter ID (EPIC)',     'STRING',  TRUE,  '^[A-Z]{3}[0-9]{7}$', 10, 10, 'Voter ID must be 3 letters followed by 7 digits (e.g. ABC1234567).'),
('patient', 'idNumber_DRIVING_LICENSE', 'Driving License Number', 'STRING', TRUE, '^[A-Z]{2}[0-9]{13}$', 15, 15, 'Driving license must be state code + 13 digits (e.g. MP0920190012345).'),
('patient', 'dob',               'Date of Birth',       'DATE',    TRUE,  NULL, NULL, NULL, 'Enter a valid date of birth that is not in the future (age up to 130 years).'),
('patient', 'gender',            'Gender',              'STRING',  TRUE,  '^(MALE|FEMALE|OTHER)$', NULL, 10, 'Please select a gender.'),
('patient', 'contactNumber',     'Contact Number',      'PHONE',   TRUE,  '^[6-9][0-9]{9}$', 10, 10, 'Contact number must be a valid 10-digit Indian mobile number.'),
('patient', 'address',           'Address',             'STRING',  FALSE, '^[^<>]*$', NULL, 255, 'Address can be up to 255 characters and must not contain < or >.'),

('staff',   'username',          'Username',            'STRING',  TRUE,  '^[a-z][a-z0-9._]*$', 4, 30, 'Username must be 4-30 lowercase letters, digits, dots or underscores and start with a letter.'),
('staff',   'password',          'Password',            'STRING',  TRUE,  '^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).*$', 10, 64, 'Password must be 10-64 characters with upper and lower case letters, a digit and a symbol.'),
('staff',   'fullName',          'Full Name',           'STRING',  TRUE,  '^[A-Za-z][A-Za-z .''-]*$', 2, 100, 'Full name must be 2-100 letters.'),
('staff',   'email',             'Email',               'EMAIL',   FALSE, '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$', NULL, 120, 'Enter a valid email address.'),
('staff',   'phoneNumber',       'Phone Number',        'PHONE',   FALSE, '^[6-9][0-9]{9}$', 10, 10, 'Phone number must be a valid 10-digit Indian mobile number.'),

('doctor',  'name',              'Doctor Name',         'STRING',  TRUE,  '^[A-Za-z][A-Za-z .''-]*$', 2, 100, 'Doctor name must be 2-100 letters.'),
('doctor',  'specialization',    'Specialization',      'STRING',  TRUE,  '^[^<>]*$', 2, 100, 'Specialization must be 2-100 characters.'),
('doctor',  'qualification',     'Qualification',       'STRING',  FALSE, '^[^<>]*$', NULL, 150, 'Qualification can be up to 150 characters.'),
('doctor',  'experienceYears',   'Experience (years)',  'NUMBER',  FALSE, '^[0-9]{1,2}$', NULL, 2, 'Experience must be a whole number between 0 and 70.'),

('service', 'serviceName',       'Service Name',        'STRING',  TRUE,  '^[^<>]*$', 2, 120, 'Service name must be 2-120 characters.'),
('service', 'category',          'Category',            'STRING',  TRUE,  '^[^<>]*$', 2, 60, 'Category must be 2-60 characters.'),
('service', 'cost',              'Cost (INR)',          'DECIMAL', TRUE,  '^[0-9]{1,9}(\.[0-9]{1,2})?$', NULL, 12, 'Cost must be a non-negative amount with up to 2 decimal places.'),
('service', 'description',       'Description',         'STRING',  FALSE, '^[^<>]*$', NULL, 500, 'Description can be up to 500 characters.'),

('invoice', 'quantity',          'Quantity',            'NUMBER',  TRUE,  '^[1-9][0-9]{0,2}$', NULL, 3, 'Quantity must be between 1 and 999.'),
('invoice', 'discount',          'Discount (INR)',      'DECIMAL', FALSE, '^[0-9]{1,9}(\.[0-9]{1,2})?$', NULL, 12, 'Discount must be a non-negative amount with up to 2 decimal places.'),
('invoice', 'paidAmount',        'Amount Paid (INR)',   'DECIMAL', TRUE,  '^[0-9]{1,9}(\.[0-9]{1,2})?$', NULL, 12, 'Paid amount must be a non-negative amount with up to 2 decimal places.'),

('cms',     'contentText',       'Content',             'STRING',  TRUE,  '^[^<>]*$', 1, 4000, 'Content must be 1-4000 characters and must not contain < or >.');

-- ---------------------------------------------------------------------------
-- Charges sheet (illustrative INR prices)
-- ---------------------------------------------------------------------------
INSERT INTO hospital_services (service_name, category, cost, description) VALUES
('OPD Consultation - General Physician', 'Consultation', 400.00, 'Outpatient evaluation by a general physician.'),
('OPD Consultation - Specialist',        'Consultation', 700.00, 'Outpatient evaluation by a specialist consultant.'),
('Emergency Consultation',               'Emergency',    1000.00, '24x7 emergency department assessment.'),
('Complete Blood Count (CBC)',           'Laboratory',   350.00, 'Haemoglobin, RBC, WBC, platelets and indices.'),
('Lipid Profile',                        'Laboratory',   600.00, 'Cholesterol, triglycerides, HDL and LDL.'),
('Blood Sugar (Fasting/PP)',             'Laboratory',   120.00, 'Fasting or post-prandial glucose.'),
('Thyroid Profile (T3, T4, TSH)',        'Laboratory',   550.00, 'Thyroid function panel.'),
('X-Ray (Single View)',                  'Radiology',    450.00, 'Digital radiograph, single view.'),
('Ultrasound - Abdomen',                 'Radiology',    1200.00, 'Whole abdomen sonography.'),
('ECG',                                  'Cardiology',   300.00, '12-lead electrocardiogram.'),
('2D Echocardiography',                  'Cardiology',   2200.00, 'Cardiac ultrasound study.'),
('Dressing (Minor)',                     'Procedures',   250.00, 'Minor wound dressing.'),
('Injection Administration',             'Procedures',   100.00, 'IM/IV injection administration charge.'),
('General Ward Bed (per day)',           'Inpatient',    1500.00, 'General ward bed, nursing and basic care per day.'),
('Private Room (per day)',               'Inpatient',    3500.00, 'Private room with attendant bed per day.'),
('ICU Bed (per day)',                    'Inpatient',    8000.00, 'Intensive care bed with monitoring per day.'),
('Physiotherapy Session',                'Rehabilitation', 500.00, 'One physiotherapy session.'),
('Ambulance (within city)',              'Emergency',    800.00, 'Ambulance transfer within Indore city limits.');

-- ---------------------------------------------------------------------------
-- Doctors (placeholder profiles)
-- ---------------------------------------------------------------------------
INSERT INTO doctors (name, specialization, qualification, experience_years) VALUES
('Dr. A. Sharma',  'General Medicine',     'MBBS, MD (Medicine)',         14),
('Dr. R. Verma',   'Cardiology',           'MBBS, MD, DM (Cardiology)',   12),
('Dr. S. Patel',   'Orthopaedics',         'MBBS, MS (Ortho)',            10),
('Dr. N. Joshi',   'Obstetrics & Gynaecology', 'MBBS, MS (OBG)',          11),
('Dr. P. Mehta',   'Paediatrics',          'MBBS, MD (Paediatrics)',       9),
('Dr. K. Rathore', 'General Surgery',      'MBBS, MS (General Surgery)',  15);

-- ---------------------------------------------------------------------------
-- CMS text blocks
-- ---------------------------------------------------------------------------
INSERT INTO cms_content (section_key, label, content_text) VALUES
('hero_title',      'Hero title',        'Compassionate care, close to home.'),
('hero_tagline',    'Hero tagline',      'Multi-speciality care with 24x7 emergency services on Indore - Ujjain Road.'),
('about_overview',  'About - overview',  'New Life Hospital is a multi-speciality hospital on Indore - Ujjain Road serving Indore, Ujjain and the surrounding communities. Our team of experienced doctors, nurses and technicians provides outpatient, inpatient, diagnostic and emergency care under one roof.'),
('mission',         'Mission',           'To deliver safe, ethical and affordable healthcare with dignity and compassion for every patient.'),
('vision',          'Vision',            'To be the most trusted hospital in the Indore - Ujjain region, known for clinical excellence and patient-first care.'),
('emergency_info',  'Emergency details', 'Our emergency department and ambulance service operate 24 hours a day, 7 days a week. In a medical emergency, call our emergency line or dial 108 for the national ambulance service.'),
('emergency_phone', 'Emergency phone',   '+91 XXXXX XXXXX'),
('contact_phone',   'Reception phone',   '+91 XXXXX XXXXX'),
('contact_email',   'Contact email',     'info@example.com'),
('address',         'Address',           'New Life Hospital, Indore - Ujjain Rd, Indore, Madhya Pradesh, India'),
('visiting_hours',  'Visiting hours',    'OPD: Mon-Sat, 9:00 AM - 8:00 PM | Visitors: 11:00 AM - 1:00 PM and 5:00 PM - 7:00 PM'),
('map_query',       'Map search text',   'New Life Hospital Indore Ujjain Road');
