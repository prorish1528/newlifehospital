import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  Doctor,
  FormRules,
  GENDER_LABELS,
  HospitalService,
  PatientSummary,
  PaymentStatus,
  Problem,
} from '../../core/models';
import { PublicApi, StaffApi } from '../../core/services/api.service';
import { AuthStore } from '../../core/services/auth.store';
import { ToastService } from '../../core/services/toast.service';
import { ValidationService, touchAll } from '../../core/services/validation.service';
import { FieldError } from '../../shared/field-error';
import { Modal } from '../../shared/modal';
import { PatientPicker } from '../../shared/patient-picker';
import { PatientRegisterForm } from '../../shared/patient-register-form';
import { money } from '../../shared/util';

type ItemGroup = FormGroup<{ serviceId: FormControl<number | null>; quantity: FormControl<number> }>;

@Component({
  selector: 'app-billing',
  imports: [ReactiveFormsModule, CurrencyPipe, FieldError, Modal, PatientPicker, PatientRegisterForm],
  templateUrl: './billing.html',
})
export class Billing implements OnInit {
  /** Optional `?patientId=` to preselect a patient. */
  readonly patientId = input<string>();

  private readonly fb = inject(FormBuilder);
  private readonly staffApi = inject(StaffApi);
  private readonly publicApi = inject(PublicApi);
  private readonly validation = inject(ValidationService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthStore);

  protected readonly genderLabels = GENDER_LABELS;
  protected readonly patient = signal<PatientSummary | null>(null);
  protected readonly doctors = signal<Doctor[]>([]);
  protected readonly services = signal<HospitalService[]>([]);
  protected readonly rules = signal<FormRules>({});
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showRegister = signal(false);

  protected readonly items = new FormArray<ItemGroup>([]);
  protected readonly form = this.fb.group({
    doctorId: this.fb.control<number | null>(null),
    items: this.items,
    discount: this.fb.nonNullable.control<string>(''),
    paymentStatus: this.fb.nonNullable.control<PaymentStatus>('FULL'),
    paidAmount: this.fb.nonNullable.control<string>(''),
  });

  private readonly value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly categories = computed(() => {
    const map = new Map<string, HospitalService[]>();
    for (const s of this.services()) map.set(s.category, [...(map.get(s.category) ?? []), s]);
    return [...map.entries()];
  });
  private readonly serviceById = computed(() => new Map(this.services().map((s) => [s.id, s])));

  protected readonly lines = computed(() => {
    const byId = this.serviceById();
    return (this.value().items ?? []).map((i) => {
      const s = i?.serviceId != null ? byId.get(Number(i.serviceId)) : undefined;
      const qty = Number(i?.quantity) > 0 ? Number(i?.quantity) : 0;
      return { unit: s?.cost ?? 0, amount: money((s?.cost ?? 0) * qty) };
    });
  });
  protected readonly total = computed(() => money(this.lines().reduce((sum, l) => sum + l.amount, 0)));
  protected readonly discount = computed(() => money(Math.max(0, Number(this.value().discount) || 0)));
  protected readonly net = computed(() => money(Math.max(0, this.total() - this.discount())));
  protected readonly paid = computed(() =>
    this.value().paymentStatus === 'FULL' ? this.net() : money(Math.max(0, Number(this.value().paidAmount) || 0)),
  );
  protected readonly balance = computed(() => money(Math.max(0, this.net() - this.paid())));
  protected readonly isPartial = computed(() => this.value().paymentStatus === 'PARTIAL');

  ngOnInit(): void {
    forkJoin({
      services: this.publicApi.services(),
      doctors: this.publicApi.doctors(),
      rules: this.validation.rules('invoice'),
    }).subscribe({
      next: ({ services, doctors, rules }) => {
        this.services.set(services);
        this.doctors.set(doctors);
        this.rules.set(rules);
        this.validation.applyRule(this.form.controls.discount, rules['discount']);
        this.form.controls.paymentStatus.valueChanges.subscribe(() => this.applyPaidRule());
        this.applyPaidRule();
        this.addItem();
        this.loading.set(false);
      },
      error: (p: Problem) => {
        this.error.set(p.detail);
        this.loading.set(false);
      },
    });
    const id = Number(this.patientId());
    if (id) {
      this.staffApi.patient(id).subscribe({ next: (p) => this.patient.set(p), error: () => {} });
    }
  }

  protected addItem(serviceId: number | null = null): void {
    const group: ItemGroup = this.fb.group({
      serviceId: this.fb.control<number | null>(serviceId),
      quantity: this.fb.nonNullable.control(1),
    });
    const rules = this.rules();
    group.controls.serviceId.setValidators((c) => (c.value == null ? { required: 'Select a service.' } : null));
    if (rules['quantity']) this.validation.applyRule(group.controls.quantity, rules['quantity']);
    this.items.push(group);
  }

  protected removeItem(index: number): void {
    this.items.removeAt(index);
  }

  protected isUsed(serviceId: number, index: number): boolean {
    return this.items.controls.some((g, i) => i !== index && Number(g.controls.serviceId.value) === serviceId);
  }

  protected onRegistered(p: PatientSummary): void {
    this.patient.set(p);
    this.showRegister.set(false);
  }

  protected submit(): void {
    this.error.set(null);
    const patient = this.patient();
    if (!patient) {
      this.error.set('Select or register a patient first.');
      return;
    }
    if (this.items.length === 0) {
      this.error.set('Add at least one service.');
      return;
    }
    if (!touchAll(this.form)) return;
    if (this.discount() > this.total()) {
      this.form.controls.discount.setErrors({ custom: 'Discount cannot exceed the total amount.' });
      return;
    }
    if (this.isPartial() && this.paid() >= this.net()) {
      this.form.controls.paidAmount.setErrors({ custom: 'For a partial payment the amount must be less than the net amount. Choose Fully Paid instead.' });
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.staffApi
      .createInvoice({
        patientId: patient.id,
        doctorId: v.doctorId ? Number(v.doctorId) : null,
        items: v.items.map((i) => ({ serviceId: Number(i.serviceId), quantity: Number(i.quantity) })),
        discount: this.discount(),
        paymentStatus: v.paymentStatus,
        paidAmount: v.paymentStatus === 'PARTIAL' ? this.paid() : null,
      })
      .subscribe({
        next: (inv) => {
          this.toast.success(`Invoice ${inv.invoiceNumber} created.`);
          this.router.navigate(['/staff/invoices', inv.id], { queryParams: { print: 1 } });
        },
        error: (p: Problem) => {
          this.saving.set(false);
          const fieldErrors = p.errors ? Object.values(p.errors).join(' ') : '';
          this.error.set(fieldErrors || p.detail);
        },
      });
  }

  private applyPaidRule(): void {
    const control = this.form.controls.paidAmount;
    const rule = this.rules()['paidAmount'];
    if (this.form.controls.paymentStatus.value === 'PARTIAL' && rule) {
      this.validation.applyRule(control, rule);
    } else {
      control.clearValidators();
      control.updateValueAndValidity({ emitEvent: false });
    }
  }
}
