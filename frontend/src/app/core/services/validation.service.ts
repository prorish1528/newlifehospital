import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Observable, map, shareReplay } from 'rxjs';
import { FieldRule, FormRules } from '../models';

/**
 * Client half of the database-driven validation. Rules come from `field_validations` via `/api/validations`
 * and are turned into Angular validators at runtime, so the browser and the Spring backend always enforce the
 * same regex, length, type and required constraints, and show the same messages.
 */
@Injectable({ providedIn: 'root' })
export class ValidationService {
  private readonly http = inject(HttpClient);
  private readonly cache = new Map<string, Observable<FormRules>>();

  /** Rules of one form keyed by field name; cached for the lifetime of the app. */
  rules(formName: string): Observable<FormRules> {
    let cached = this.cache.get(formName);
    if (!cached) {
      cached = this.http.get<FieldRule[]>('/api/validations', { params: { forms: formName } }).pipe(
        map((list) => Object.fromEntries(list.map((r) => [r.fieldName, r]))),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
      this.cache.set(formName, cached);
    }
    return cached;
  }

  /**
   * Applies rules to the controls of `group`. `ruleFor` maps a control name to a different rule name
   * (e.g. `idNumber` -> `idNumber_AADHAAR`); controls without a rule are left untouched.
   */
  apply(group: FormGroup, rules: FormRules, ruleFor: Record<string, string> = {}): void {
    for (const [name, control] of Object.entries(group.controls)) {
      const rule = rules[ruleFor[name] ?? name];
      if (rule) {
        this.applyRule(control, rule);
      }
    }
  }

  applyRule(control: AbstractControl, rule: FieldRule, extra: ValidatorFn[] = []): void {
    control.setValidators([ruleValidator(rule), ...extra]);
    control.updateValueAndValidity({ emitEvent: false });
  }
}

/** One validator per rule. Errors: `{required: label}` or `{rule: message}`. */
export function ruleValidator(rule: FieldRule): ValidatorFn {
  const regex = rule.regexPattern ? new RegExp(rule.regexPattern) : null;
  return (control: AbstractControl): ValidationErrors | null => {
    const raw = control.value;
    const value = raw === null || raw === undefined ? '' : String(raw);
    if (value.trim() === '') {
      return rule.required ? { required: `${rule.fieldLabel} is required.` } : null;
    }
    const invalid = { rule: rule.errorMessage };
    if (rule.minLength != null && value.length < rule.minLength) return invalid;
    if (rule.maxLength != null && value.length > rule.maxLength) return invalid;
    if (regex && !regex.test(value)) return invalid;
    switch (rule.dataType) {
      case 'NUMBER':
        return /^-?\d+$/.test(value) ? null : invalid;
      case 'DECIMAL':
        return Number.isFinite(Number(value)) && Number(value) >= 0 ? null : invalid;
      case 'DATE':
        return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(Date.parse(value)) ? null : invalid;
      default:
        return null;
    }
  };
}

/** Marks every control touched and returns whether the group is valid. */
export function touchAll(group: FormGroup): boolean {
  group.markAllAsTouched();
  return group.valid;
}

/** Copies server-side field errors (from a 400 Problem response) onto matching controls. */
export function applyServerErrors(group: FormGroup, errors: Record<string, string> | undefined): boolean {
  if (!errors) return false;
  let applied = false;
  for (const [field, message] of Object.entries(errors)) {
    const control = group.get(field);
    if (control) {
      control.setErrors({ server: message });
      control.markAsTouched();
      applied = true;
    }
  }
  return applied;
}
