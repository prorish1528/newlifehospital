import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { GENDER_LABELS, InvoiceDetail, Problem } from '../../core/models';
import { AdminApi, PublicApi, StaffApi } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { ValidationService } from '../../core/services/validation.service';
import { FieldError } from '../../shared/field-error';
import { Logo } from '../../layout/logo';
import { toSignal } from '@angular/core/rxjs-interop';

/** Printable invoice. Staff can record further payments against an outstanding balance. */
@Component({
  selector: 'app-invoice-view',
  imports: [CurrencyPipe, DatePipe, RouterLink, ReactiveFormsModule, FieldError, Logo],
  templateUrl: './invoice-view.html',
})
export class InvoiceView implements OnInit {
  readonly id = input.required<string>();
  readonly scope = input<'staff' | 'admin'>('staff');
  readonly print = input<string>();

  private readonly staffApi = inject(StaffApi);
  private readonly adminApi = inject(AdminApi);
  private readonly validation = inject(ValidationService);
  private readonly toast = inject(ToastService);
  protected readonly cms = toSignal(inject(PublicApi).cms(), {
    initialValue: {} as Record<string, string>,
  });

  protected readonly genderLabels = GENDER_LABELS;
  protected readonly invoice = signal<InvoiceDetail | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly paying = signal(false);
  protected readonly payment = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    const id = Number(this.id());
    const call = this.scope() === 'admin' ? this.adminApi.invoice(id) : this.staffApi.invoice(id);
    call.subscribe({
      next: (inv) => {
        this.invoice.set(inv);
        if (this.print()) setTimeout(() => window.print(), 300);
      },
      error: (p: Problem) => this.error.set(p.detail),
    });
    this.validation.rules('invoice').subscribe((rules) => {
      if (rules['paidAmount']) this.validation.applyRule(this.payment, rules['paidAmount']);
    });
  }

  protected doPrint(): void {
    window.print();
  }

  protected recordPayment(): void {
    const inv = this.invoice();
    this.payment.markAsTouched();
    if (!inv || this.payment.invalid) return;
    const amount = Number(this.payment.value);
    if (amount <= 0 || amount > inv.balanceAmount) {
      this.payment.setErrors({ custom: `Enter an amount between 0.01 and ${inv.balanceAmount}.` });
      return;
    }
    this.paying.set(true);
    this.staffApi.recordPayment(inv.id, amount).subscribe({
      next: (updated) => {
        this.paying.set(false);
        this.invoice.set(updated);
        this.payment.reset();
        this.toast.success('Payment recorded.');
      },
      error: (p: Problem) => {
        this.paying.set(false);
        this.payment.setErrors({ server: p.errors?.['amount'] ?? p.detail });
      },
    });
  }
}
