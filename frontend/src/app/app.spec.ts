import { FormControl } from '@angular/forms';
import { FieldRule } from './core/models';
import { ruleValidator } from './core/services/validation.service';
import { ageFrom, safeReturnUrl } from './shared/util';

const aadhaar: FieldRule = {
  formName: 'patient',
  fieldName: 'idNumber_AADHAAR',
  fieldLabel: 'Aadhaar Number',
  dataType: 'STRING',
  required: true,
  regexPattern: '^[2-9][0-9]{11}$',
  minLength: 12,
  maxLength: 12,
  errorMessage: 'Aadhaar number must be 12 digits and cannot start with 0 or 1.',
};

describe('ruleValidator (database-driven validation)', () => {
  const validate = (value: unknown, rule = aadhaar) => ruleValidator(rule)(new FormControl(value));

  it('reports required with the rule label', () => {
    expect(validate('')).toEqual({ required: 'Aadhaar Number is required.' });
  });

  it('applies regex and length rules with the configured message', () => {
    expect(validate('123456789012')).toEqual({ rule: aadhaar.errorMessage });
    expect(validate('23456789012')).toEqual({ rule: aadhaar.errorMessage });
    expect(validate('234567890123')).toBeNull();
  });

  it('checks decimal data types', () => {
    const cost: FieldRule = { ...aadhaar, dataType: 'DECIMAL', regexPattern: '^[0-9]{1,9}(\\.[0-9]{1,2})?$', minLength: undefined, maxLength: 12 };
    expect(validate('450.50', cost)).toBeNull();
    expect(validate('450.505', cost)).not.toBeNull();
  });
});

describe('util', () => {
  it('computes completed years of age', () => {
    const today = new Date(2026, 9, 12);
    expect(ageFrom('1990-10-12', today)).toBe(36);
    expect(ageFrom('1990-10-13', today)).toBe(35);
    expect(ageFrom('2030-01-01', today)).toBeNull();
  });

  it('blocks open redirects after login', () => {
    expect(safeReturnUrl('/staff/billing', '/staff')).toBe('/staff/billing');
    expect(safeReturnUrl('//evil.example', '/staff')).toBe('/staff');
    expect(safeReturnUrl('https://evil.example', '/staff')).toBe('/staff');
  });
});
