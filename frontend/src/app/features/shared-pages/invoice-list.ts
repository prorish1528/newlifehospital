import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, catchError, of } from 'rxjs';
import { InvoiceSummary, Page, PaymentStatus } from '../../core/models';
import { AdminApi, InvoiceFilter, StaffApi } from '../../core/services/api.service';

/** Invoice repository view; `scope` (route data) selects the staff or admin API. */
@Component({
  selector: 'app-invoice-list',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  template: `
    <div class="page-title flex-between">
      <div>
        <h1>{{ scope() === 'admin' ? 'All Invoices' : 'Invoices' }}</h1>
        <p class="muted">Search by invoice number, patient name or patient code.</p>
      </div>
      @if (scope() === 'staff') {<a class="btn btn-primary" routerLink="/staff/billing">＋ New invoice</a>}
    </div>

    <form class="card filter-bar" [formGroup]="filters" (ngSubmit)="search(0)">
      <input type="search" formControlName="q" placeholder="Invoice no. / patient" aria-label="Search" />
      <label>From <input type="date" formControlName="from" /></label>
      <label>To <input type="date" formControlName="to" /></label>
      <select formControlName="status" aria-label="Payment status">
        <option value="">All statuses</option>
        <option value="FULL">Fully Paid</option>
        <option value="PARTIAL">Partially Paid</option>
      </select>
      <button type="submit" class="btn btn-primary">Search</button>
      <button type="button" class="btn btn-ghost" (click)="filters.reset(); search(0)">Reset</button>
    </form>

    <div class="card">
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>Invoice No.</th><th>Date</th><th>Patient</th><th>Doctor</th><th>Billed by</th>
              <th class="right">Net</th><th class="right">Paid</th><th class="right">Balance</th><th>Status</th></tr>
          </thead>
          <tbody>
            @for (i of page()?.content ?? []; track i.id) {
              <tr class="row-link" [routerLink]="[i.id]">
                <td class="mono"><a [routerLink]="[i.id]">{{ i.invoiceNumber }}</a></td>
                <td>{{ i.invoiceDate | date: 'dd MMM yyyy' }}</td>
                <td>{{ i.patientName }} <small class="muted mono">{{ i.patientCode }}</small></td>
                <td>{{ i.doctorName ?? '—' }}</td>
                <td>{{ i.staffName }}</td>
                <td class="right">{{ i.netAmount | currency: 'INR' }}</td>
                <td class="right">{{ i.paidAmount | currency: 'INR' }}</td>
                <td class="right" [class.text-danger]="i.balanceAmount > 0">{{ i.balanceAmount | currency: 'INR' }}</td>
                <td><span class="status-pill" [class.status-partial]="i.paymentStatus === 'PARTIAL'">
                  {{ i.paymentStatus === 'FULL' ? 'Fully Paid' : 'Partial' }}</span></td>
              </tr>
            } @empty {
              <tr><td colspan="9" class="muted center">{{ loading() ? 'Loading…' : 'No invoices found.' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
      @if (page(); as p) {
        @if (p.totalPages > 1) {
          <div class="pager">
            <button type="button" class="btn btn-sm btn-ghost" [disabled]="p.page === 0" (click)="search(p.page - 1)">‹ Prev</button>
            <span>Page {{ p.page + 1 }} of {{ p.totalPages }} · {{ p.totalElements }} invoices</span>
            <button type="button" class="btn btn-sm btn-ghost" [disabled]="p.page + 1 >= p.totalPages" (click)="search(p.page + 1)">Next ›</button>
          </div>
        }
      }
    </div>
  `,
})
export class InvoiceList implements OnInit {
  readonly scope = input<'staff' | 'admin'>('staff');
  readonly patientId = input<string>();

  private readonly staffApi = inject(StaffApi);
  private readonly adminApi = inject(AdminApi);
  protected readonly filters = inject(FormBuilder).nonNullable.group({
    q: '',
    from: '',
    to: '',
    status: '' as PaymentStatus | '',
  });
  protected readonly page = signal<Page<InvoiceSummary> | null>(null);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.search(0);
  }

  protected search(pageNo: number): void {
    const f: InvoiceFilter = { ...this.filters.getRawValue(), page: pageNo, size: 20 };
    if (this.patientId()) f.patientId = Number(this.patientId());
    this.loading.set(true);
    const call: Observable<Page<InvoiceSummary>> =
      this.scope() === 'admin' ? this.adminApi.invoices(f) : this.staffApi.invoices(f);
    call.pipe(catchError(() => of(null))).subscribe((p) => {
      this.page.set(p);
      this.loading.set(false);
    });
  }
}
