import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { HospitalService, Problem } from '../../core/models';
import { AdminApi } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { ValidationService, applyServerErrors, touchAll } from '../../core/services/validation.service';
import { FieldError } from '../../shared/field-error';
import { Modal } from '../../shared/modal';

@Component({
  selector: 'app-charges',
  imports: [ReactiveFormsModule, CurrencyPipe, Modal, FieldError],
  template: `
    <div class="page-title flex-between">
      <div>
        <h1>Charges Sheet</h1>
        <p class="muted">Master list of billable services. Price changes apply to new invoices only.</p>
      </div>
      <button type="button" class="btn btn-primary" (click)="edit(null)">＋ Add service</button>
    </div>
    <div class="card">
      <div class="toolbar">
        <input type="search" class="search-input" placeholder="Filter services…" aria-label="Filter services"
               [value]="filter()" (input)="filter.set($any($event.target).value)" />
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>Service</th><th>Category</th><th class="right">Cost</th><th>Status</th><th></th></tr></thead>
          <tbody>
            @for (s of visible(); track s.id) {
              <tr [class.row-muted]="!s.active">
                <td><strong>{{ s.serviceName }}</strong>@if (s.description) {<br /><small class="muted">{{ s.description }}</small>}</td>
                <td>{{ s.category }}</td>
                <td class="right">{{ s.cost | currency: 'INR' }}</td>
                <td><span class="status-pill" [class.status-off]="!s.active">{{ s.active ? 'Active' : 'Inactive' }}</span></td>
                <td class="right"><button type="button" class="btn btn-sm btn-outline" (click)="edit(s)">Edit</button></td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="muted center">No services.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>

    <app-modal [open]="open()" [title]="editing() ? 'Edit service' : 'Add service'" (closed)="open.set(false)">
      <form [formGroup]="form" (ngSubmit)="save()" novalidate>
        @if (error(); as e) {<div class="alert alert-error">{{ e }}</div>}
        <div class="field"><label for="s-name">Service name *</label><input id="s-name" formControlName="serviceName" />
          <app-field-error [control]="form.controls.serviceName" /></div>
        <div class="field"><label for="s-cat">Category *</label><input id="s-cat" formControlName="category" list="cats" />
          <datalist id="cats">@for (c of categories(); track c) {<option [value]="c"></option>}</datalist>
          <app-field-error [control]="form.controls.category" /></div>
        <div class="field"><label for="s-cost">Cost (INR) *</label><input id="s-cost" formControlName="cost" inputmode="decimal" />
          <app-field-error [control]="form.controls.cost" /></div>
        <div class="field"><label for="s-desc">Description</label><textarea id="s-desc" rows="2" formControlName="description"></textarea>
          <app-field-error [control]="form.controls.description" /></div>
        <label class="checkbox"><input type="checkbox" formControlName="active" /> Available for billing</label>
        <div class="form-actions"><button type="button" class="btn btn-ghost" (click)="open.set(false)">Cancel</button>
          <button type="submit" class="btn btn-primary" [disabled]="busy()">Save</button></div>
      </form>
    </app-modal>
  `,
})
export class Charges implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly validation = inject(ValidationService);
  private readonly toast = inject(ToastService);

  protected readonly services = signal<HospitalService[]>([]);
  protected readonly filter = signal('');
  protected readonly open = signal(false);
  protected readonly editing = signal<HospitalService | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    serviceName: '',
    category: '',
    cost: '',
    description: '',
    active: true,
  });

  protected readonly categories = computed(() => [...new Set(this.services().map((s) => s.category))].sort());
  protected readonly visible = computed(() => {
    const q = this.filter().trim().toLowerCase();
    return this.services().filter((s) => !q || `${s.serviceName} ${s.category}`.toLowerCase().includes(q));
  });

  ngOnInit(): void {
    this.validation.rules('service').subscribe((r) => this.validation.apply(this.form, r));
    this.load();
  }

  private load(): void {
    this.api.services().subscribe((s) => this.services.set(s));
  }

  protected edit(s: HospitalService | null): void {
    this.error.set(null);
    this.editing.set(s);
    this.form.reset(
      s
        ? { serviceName: s.serviceName, category: s.category, cost: String(s.cost), description: s.description ?? '', active: s.active }
        : undefined,
    );
    this.open.set(true);
  }

  protected save(): void {
    if (!touchAll(this.form)) return;
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.api
      .saveService(this.editing()?.id ?? null, { ...v, cost: Number(v.cost), description: v.description || null })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.open.set(false);
          this.toast.success('Charges sheet updated.');
          this.load();
        },
        error: (p: Problem) => {
          this.busy.set(false);
          if (!applyServerErrors(this.form, p.errors)) this.error.set(p.detail);
        },
      });
  }
}
