import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Dashboard, Problem, Totals } from '../../core/models';
import { AdminApi } from '../../core/services/api.service';
import { RevenueChart } from './revenue-chart';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

@Component({
  selector: 'app-admin-dashboard',
  imports: [CurrencyPipe, RouterLink, RevenueChart],
  template: `
    <div class="page-title flex-between">
      <div>
        <h1>Financial &amp; Operational Dashboard</h1>
        <p class="muted">Billed = net invoice value · Collected = amount paid on invoices raised in the period.</p>
      </div>
      <span class="btn-row">
        <a class="btn btn-outline" routerLink="/admin/invoices">All invoices</a>
        <button type="button" class="btn btn-ghost" (click)="load()">↻ Refresh</button>
      </span>
    </div>

    @if (error(); as e) {<div class="alert alert-error">{{ e }}</div>}
    @if (data(); as d) {
      <div class="kpi-grid">
        @for (k of kpis(d); track k.label) {
          <section class="card kpi">
            <h2 class="kpi-label">{{ k.label }}</h2>
            <div class="kpi-value">{{ k.t.billed | currency: 'INR' : 'symbol' : '1.0-0' }}</div>
            <div class="kpi-sub">billed · {{ k.t.invoiceCount }} invoice{{ k.t.invoiceCount === 1 ? '' : 's' }}</div>
            <dl class="kpi-split">
              <div><dt>Collected</dt><dd>{{ k.t.collected | currency: 'INR' : 'symbol' : '1.0-0' }}</dd></div>
              <div><dt>Outstanding</dt><dd [class.text-danger]="k.t.outstanding > 0">{{ k.t.outstanding | currency: 'INR' : 'symbol' : '1.0-0' }}</dd></div>
            </dl>
          </section>
        }
      </div>

      <section class="card"><app-revenue-chart title="Daily revenue — last 30 days" [points]="d.daily" [fmt]="fmtDay" [fmtShort]="fmtDayShort" /></section>
      <div class="two-col">
        <section class="card"><app-revenue-chart title="Monthly revenue — last 12 months" [points]="d.monthly" [fmt]="fmtMonth" [fmtShort]="fmtMonthShort" /></section>
        <section class="card"><app-revenue-chart title="Yearly revenue" [points]="d.yearly" /></section>
      </div>
    } @else if (!error()) {
      <p class="muted">Loading dashboard…</p>
    }
  `,
})
export class AdminDashboard {
  private readonly api = inject(AdminApi);
  protected readonly data = signal<Dashboard | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly fmtDay = (l: string) => {
    const [y, m, d] = l.split('-');
    return `${Number(d)} ${MONTHS[Number(m) - 1]} ${y}`;
  };
  protected readonly fmtDayShort = (l: string) => {
    const [, m, d] = l.split('-');
    return `${Number(d)} ${MONTHS[Number(m) - 1]}`;
  };
  protected readonly fmtMonth = (l: string) => {
    const [y, m] = l.split('-');
    return `${MONTHS[Number(m) - 1]} ${y}`;
  };
  protected readonly fmtMonthShort = (l: string) => {
    const [y, m] = l.split('-');
    return `${MONTHS[Number(m) - 1]} ${y.slice(2)}`;
  };

  constructor() {
    this.load();
  }

  protected load(): void {
    this.error.set(null);
    this.api.dashboard().subscribe({
      next: (d) => this.data.set(d),
      error: (p: Problem) => this.error.set(p.detail),
    });
  }

  protected kpis(d: Dashboard): { label: string; t: Totals }[] {
    return [
      { label: 'Today', t: d.today },
      { label: 'This month', t: d.thisMonth },
      { label: 'This year', t: d.thisYear },
    ];
  }
}
