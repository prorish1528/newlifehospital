-- Patient visit date/time on each invoice. Existing invoices take their billing time as the visit time.
ALTER TABLE invoices ADD COLUMN visit_at TIMESTAMP WITH TIME ZONE;
UPDATE invoices SET visit_at = created_at;
ALTER TABLE invoices ALTER COLUMN visit_at SET NOT NULL;
