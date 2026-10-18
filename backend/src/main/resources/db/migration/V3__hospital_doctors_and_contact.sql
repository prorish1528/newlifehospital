-- Replace the placeholder doctors from V2 with New Life Hospital's doctors, and set the hospital phone number.
-- Rows are matched by the placeholder names, so anything already edited through the Admin portal is left alone.
-- experience_years holds the minimum; the website shows it as "15+ years experience".

UPDATE doctors SET name = 'Dr. Mahesh Gaur',         specialization = 'General Medicine', qualification = 'MD (Medicine)',   experience_years = 15 WHERE name = 'Dr. A. Sharma';
UPDATE doctors SET name = 'Dr. Durgesh Rathi',       specialization = 'Radiology',        qualification = NULL,              experience_years = 15 WHERE name = 'Dr. R. Verma';
UPDATE doctors SET name = 'Dr. Namrata Mishra',      specialization = 'Surgery',          qualification = NULL,              experience_years = 12 WHERE name = 'Dr. S. Patel';
UPDATE doctors SET name = 'Dr. Ashok Nanria',        specialization = 'Orthopedics',      qualification = NULL,              experience_years = 15 WHERE name = 'Dr. N. Joshi';
UPDATE doctors SET name = 'Dr. Ananda Prakash Mishra', specialization = 'Homeopathy',     qualification = 'MD (Homeopathy)', experience_years = 15 WHERE name = 'Dr. P. Mehta';
UPDATE doctors SET name = 'Dr. DayaShankar Pandey',  specialization = 'Homeopathy',       qualification = 'BHMS',            experience_years = 15 WHERE name = 'Dr. K. Rathore';

UPDATE cms_content SET content_text = '+91 7312722803', updated_at = CURRENT_TIMESTAMP
WHERE section_key IN ('contact_phone', 'emergency_phone') AND content_text = '+91 XXXXX XXXXX';
