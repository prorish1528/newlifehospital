import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Doctor, Problem } from '../../core/models';
import { AdminApi } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { ValidationService, applyServerErrors, touchAll } from '../../core/services/validation.service';
import { DoctorAvatar } from '../../shared/doctor-avatar';
import { FieldError } from '../../shared/field-error';
import { Modal } from '../../shared/modal';

@Component({
  selector: 'app-doctors-admin',
  imports: [ReactiveFormsModule, Modal, FieldError, DoctorAvatar],
  template: `
    <div class="page-title flex-between">
      <div>
        <h1>Doctor Management</h1>
        <p class="muted">Profiles shown on the public website and available for OPD selection when active.</p>
      </div>
      <button type="button" class="btn btn-primary" (click)="edit(null)">＋ Add doctor</button>
    </div>

    <div class="grid-cards">
      @for (d of doctors(); track d.id) {
        <article class="card doctor-card" [class.row-muted]="!d.active">
          <app-doctor-avatar [doctor]="d" size="lg" />
          <h3>{{ d.name }}</h3>
          <p class="specialty">{{ d.specialization }}</p>
          <p class="muted small">{{ d.qualification }}@if (d.experienceYears != null) { · {{ d.experienceYears }} yrs}</p>
          <span class="status-pill" [class.status-off]="!d.active">{{ d.active ? 'Active' : 'Hidden' }}</span>
          <div class="btn-row mt-1">
            <button type="button" class="btn btn-sm btn-outline" (click)="edit(d)">Edit</button>
            <label class="btn btn-sm btn-ghost file-btn">
              Photo<input type="file" accept="image/jpeg,image/png,image/webp" (change)="upload(d, $event)" />
            </label>
          </div>
        </article>
      }
    </div>

    <app-modal [open]="open()" [title]="editing() ? 'Edit doctor' : 'Add doctor'" (closed)="open.set(false)">
      <form [formGroup]="form" (ngSubmit)="save()" novalidate>
        @if (error(); as e) {<div class="alert alert-error">{{ e }}</div>}
        <div class="field"><label for="d-name">Name *</label><input id="d-name" formControlName="name" placeholder="Dr. Firstname Lastname" />
          <app-field-error [control]="form.controls.name" /></div>
        <div class="field"><label for="d-spec">Specialization *</label><input id="d-spec" formControlName="specialization" />
          <app-field-error [control]="form.controls.specialization" /></div>
        <div class="field"><label for="d-qual">Qualification</label><input id="d-qual" formControlName="qualification" />
          <app-field-error [control]="form.controls.qualification" /></div>
        <div class="field"><label for="d-exp">Experience (years)</label><input id="d-exp" formControlName="experienceYears" inputmode="numeric" />
          <app-field-error [control]="form.controls.experienceYears" /></div>
        <label class="checkbox"><input type="checkbox" formControlName="active" /> Show on website and in OPD selection</label>
        <div class="form-actions"><button type="button" class="btn btn-ghost" (click)="open.set(false)">Cancel</button>
          <button type="submit" class="btn btn-primary" [disabled]="busy()">Save</button></div>
      </form>
    </app-modal>
  `,
})
export class DoctorsAdmin implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly validation = inject(ValidationService);
  private readonly toast = inject(ToastService);

  protected readonly doctors = signal<Doctor[]>([]);
  protected readonly open = signal(false);
  protected readonly editing = signal<Doctor | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: '',
    specialization: '',
    qualification: '',
    experienceYears: '',
    active: true,
  });

  ngOnInit(): void {
    this.validation.rules('doctor').subscribe((r) => this.validation.apply(this.form, r));
    this.load();
  }

  private load(): void {
    this.api.doctors().subscribe((d) => this.doctors.set(d));
  }

  protected edit(d: Doctor | null): void {
    this.error.set(null);
    this.editing.set(d);
    this.form.reset(
      d
        ? {
            name: d.name,
            specialization: d.specialization,
            qualification: d.qualification ?? '',
            experienceYears: d.experienceYears == null ? '' : String(d.experienceYears),
            active: d.active,
          }
        : undefined,
    );
    this.open.set(true);
  }

  protected save(): void {
    if (!touchAll(this.form)) return;
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.api
      .saveDoctor(this.editing()?.id ?? null, {
        ...v,
        qualification: v.qualification || null,
        experienceYears: v.experienceYears === '' ? null : Number(v.experienceYears),
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.open.set(false);
          this.toast.success('Doctor profile saved.');
          this.load();
        },
        error: (p: Problem) => {
          this.busy.set(false);
          if (!applyServerErrors(this.form, p.errors)) this.error.set(p.detail);
        },
      });
  }

  protected upload(d: Doctor, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      this.toast.error('Image must be 2 MB or smaller.');
      return;
    }
    this.api.uploadDoctorPhoto(d.id, file).subscribe({
      next: () => {
        this.toast.success('Photo updated.');
        this.load();
      },
      error: (p: Problem) => this.toast.error(p.detail),
    });
  }
}
