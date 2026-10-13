import { Component, DestroyRef, OnInit, computed, inject, output, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { debounceTime, distinctUntilChanged, filter, merge, of, switchMap } from 'rxjs';
import {
  FormRules,
  GENDER_LABELS,
  ID_TYPE_LABELS,
  IdType,
  PatientDetail,
  PatientSummary,
  Problem,
} from '../core/models';
import { StaffApi } from '../core/services/api.service';
import { ToastService } from '../core/services/toast.service';
import { ValidationService, applyServerErrors, touchAll } from '../core/services/validation.service';
import { FieldError } from './field-error';
import { ageLabel, isoToday } from './util';

/**
 * Patient registration with database-driven validation, live age calculation, name auto-suggest and
 * exact-ID duplicate detection. Used on the registration page and as the billing pop-up.
 */
@Component({
  selector: 'app-patient-register-form',
  imports: [ReactiveFormsModule, FieldError],
  templateUrl: './patient-register-form.html',
})
export class PatientRegisterForm implements OnInit {
  readonly registered = output<PatientDetail>();
  readonly useExisting = output<PatientSummary>();

  private readonly fb = inject(FormBuilder);
  private readonly api = inject(StaffApi);
  private readonly validation = inject(ValidationService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly idTypes = Object.entries(ID_TYPE_LABELS) as [IdType, string][];
  protected readonly genders = Object.entries(GENDER_LABELS);
  protected readonly genderLabels = GENDER_LABELS;
  protected readonly nameFocused = signal(false);
  protected readonly today = isoToday();

  protected readonly form = this.fb.nonNullable.group({
    name: '',
    idType: 'AADHAAR' as IdType,
    idTypeOther: '',
    idNumber: '',
    dob: '',
    gender: '',
    contactNumber: '',
    address: '',
  });

  protected readonly rules = signal<FormRules>({});
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly suggestions = signal<PatientSummary[]>([]);
  protected readonly duplicate = signal<PatientSummary | null>(null);

  private readonly dob = toSignal(this.form.controls.dob.valueChanges, { initialValue: '' });
  private readonly idType = toSignal(this.form.controls.idType.valueChanges, { initialValue: 'AADHAAR' as IdType });
  protected readonly age = computed(() => ageLabel(this.dob()));
  protected readonly showOther = computed(() => this.idType() === 'OTHER');
  protected readonly idRule = computed(() => this.rules()[`idNumber_${this.idType()}`] ?? this.rules()['idNumber']);

  ngOnInit(): void {
    this.validation.rules('patient').subscribe({
      next: (rules) => {
        this.rules.set(rules);
        this.validation.apply(this.form, rules, { idNumber: 'idNumber' });
        this.applyIdRules();
        this.validation.applyRule(this.form.controls.dob, rules['dob'], [dobRange]);
        this.loading.set(false);
      },
      error: (p: Problem) => this.formError.set(p.detail),
    });

    this.form.controls.idType.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.applyIdRules();
      this.duplicate.set(null);
    });

    // Name / ID auto-suggest of existing patients to prevent duplicate registrations.
    this.form.controls.name.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => (q.trim().length >= 2 ? this.api.suggest(q.trim()) : of([]))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({ next: (list) => this.suggestions.set(list), error: () => this.suggestions.set([]) });

    // Exact duplicate check on the (encrypted, blind-indexed) ID number.
    merge(this.form.controls.idNumber.valueChanges, this.form.controls.idType.valueChanges,
      this.form.controls.idTypeOther.valueChanges)
      .pipe(
        debounceTime(400),
        filter(() => this.idNumberFormatOk()),
        switchMap(() => {
          const v = this.form.getRawValue();
          return this.api.checkId(v.idType, v.idNumber, v.idTypeOther || undefined);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          const control = this.form.controls.idNumber;
          if (res.exists && res.patient) {
            this.duplicate.set(res.patient);
            control.setErrors({ ...control.errors, duplicate: `Already registered as ${res.patient.patientCode} (${res.patient.name}).` });
            control.markAsTouched();
          } else {
            this.duplicate.set(null);
          }
        },
        error: () => this.duplicate.set(null),
      });
  }

  /** Uppercases and strips spaces/hyphens as the user types, matching server-side normalisation. */
  protected normalizeId(event: Event): void {
    const input = event.target as HTMLInputElement;
    const normalized = input.value.replace(/[\s-]/g, '').toUpperCase();
    if (normalized !== input.value) {
      this.form.controls.idNumber.setValue(normalized);
    }
  }

  protected blurName(): void {
    this.nameFocused.set(false);
  }

  protected rule(field: string) {
    return this.rules()[field];
  }

  protected label(field: string, fallback: string): string {
    return this.rules()[field]?.fieldLabel ?? fallback;
  }

  protected submit(): void {
    this.formError.set(null);
    if (!touchAll(this.form) || this.duplicate()) return;
    const v = this.form.getRawValue();
    this.submitting.set(true);
    this.api
      .registerPatient({
        ...v,
        idTypeOther: v.idType === 'OTHER' ? v.idTypeOther : null,
        address: v.address || null,
      })
      .subscribe({
        next: (patient) => {
          this.submitting.set(false);
          this.toast.success(`Patient registered: ${patient.patientCode}`);
          this.form.reset();
          this.suggestions.set([]);
          this.registered.emit(patient);
        },
        error: (p: Problem) => {
          this.submitting.set(false);
          if (!applyServerErrors(this.form, p.errors)) this.formError.set(p.detail);
        },
      });
  }

  private applyIdRules(): void {
    const rules = this.rules();
    const idRule = this.idRule();
    if (idRule) this.validation.applyRule(this.form.controls.idNumber, idRule);
    if (rules['idTypeOther']) {
      const requiredWhenOther = (c: AbstractControl): ValidationErrors | null =>
        this.form.controls.idType.value === 'OTHER' && !String(c.value ?? '').trim()
          ? { required: 'Please specify the ID type.' }
          : null;
      this.validation.applyRule(this.form.controls.idTypeOther, rules['idTypeOther'], [requiredWhenOther]);
    }
  }

  private idNumberFormatOk(): boolean {
    const c = this.form.controls.idNumber;
    const { duplicate, ...others } = c.errors ?? {};
    return !!c.value && Object.keys(others).length === 0 && (this.form.controls.idType.value !== 'OTHER' || !!this.form.controls.idTypeOther.value);
  }
}

function dobRange(c: AbstractControl): ValidationErrors | null {
  const v = String(c.value ?? '');
  if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) return null;
  const today = isoToday();
  const [y, m, d] = today.split('-');
  const oldest = `${Number(y) - 130}-${m}-${d}`;
  return v > today || v < oldest ? { custom: 'Date of birth cannot be in the future or more than 130 years ago.' } : null;
}
