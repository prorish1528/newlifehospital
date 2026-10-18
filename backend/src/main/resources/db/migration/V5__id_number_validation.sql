-- ID number validation per ID type. Values are checked after normalisation (upper-cased, spaces and hyphens
-- removed), so "MP09 2019 0012345" and "mp-0920190012345" are both checked as MP0920190012345.

-- Aadhaar: exactly 16 digits, as requested by the hospital. (A standard Aadhaar number is 12 digits; the
-- 16-digit form is the Aadhaar Virtual ID.)
UPDATE field_validations
SET regex_pattern = '^[0-9]{16}$', min_length = 16, max_length = 16,
    error_message = 'Aadhaar number must be exactly 16 digits.',
    updated_at = CURRENT_TIMESTAMP
WHERE form_name = 'patient' AND field_name = 'idNumber_AADHAAR';

-- Driving licence: state code + 2-digit RTO code + 4-digit year of issue (19xx/20xx) + 7-digit serial
-- (Ministry of Road Transport format, e.g. MP09 2019 0012345). State codes include former codes still printed on
-- older licences (OR, UA, DN, TS).
UPDATE field_validations
SET regex_pattern = '^(AN|AP|AR|AS|BR|CG|CH|DD|DL|DN|GA|GJ|HP|HR|JH|JK|KA|KL|LA|LD|MH|ML|MN|MP|MZ|NL|OD|OR|PB|PY|RJ|SK|TG|TN|TR|TS|UA|UK|UP|WB)[0-9]{2}(19|20)[0-9]{2}[0-9]{7}$',
    min_length = 15, max_length = 15,
    error_message = 'Driving licence must be state code, RTO code, year of issue and 7-digit number (e.g. MP09 2019 0012345).',
    updated_at = CURRENT_TIMESTAMP
WHERE form_name = 'patient' AND field_name = 'idNumber_DRIVING_LICENSE';

-- Voter ID (EPIC): 3 letters (constituency code) + 7 digits (6-digit serial + check digit). Format unchanged,
-- message clarified.
UPDATE field_validations
SET error_message = 'Voter ID (EPIC) must be 3 letters followed by 7 digits (e.g. ABC1234567).',
    updated_at = CURRENT_TIMESTAMP
WHERE form_name = 'patient' AND field_name = 'idNumber_VOTER_ID';

-- Passport: 1 letter + 7 digits (e.g. K1234567). Format unchanged.
