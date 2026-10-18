-- Hospital motto as the home page headline (replaces the V2 placeholder unless an admin already changed it).
UPDATE cms_content SET content_text = 'नर सेवा नारायण सेवा', updated_at = CURRENT_TIMESTAMP
WHERE section_key = 'hero_title' AND content_text = 'Compassionate care, close to home.';
