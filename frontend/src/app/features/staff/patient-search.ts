import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, startWith, switchMap, catchError, of, tap } from 'rxjs';
import { GENDER_LABELS, ID_TYPE_LABELS, Page, PatientDetail, PatientSummary, Problem } from '../../core/models';
import { StaffApi } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { ValidationService, applyServerErrors, touchAll } from '../../core/services/validation.service';
import { FieldError } from '../../shared/field-error';
import { Modal } from '../../shared/modal';

@Component({
  selector: 'app-patient-search',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, Modal, FieldError],
  templateUrl: './patient-search.html',
})
export class PatientSearch implements OnInit {
  /** `?open=<id>` opens a patient directly (e.g. from a duplicate warning). */
  readonly open = input<string>();

  private readonly api = inject(StaffApi);
  private readonly validation = inject(ValidationService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly idLabels = ID_TYPE_LABELS;
  protected readonly genderLabels = GENDER_LABELS;
  protected readonly query = new FormControl('', { nonNullable: true });
  protected readonly page = signal(0);
  protected readonly result = signal<Page<PatientSummary> | null>(null);
  protected readonly loading = signal(false);

  protected readonly selected = signal<PatientDetail | null>(null);
  protected readonly saving = signal(false);
  protected readonly editError = signal<string | null>(null);
  /** Only contact number and address are editable — identity fields are displayed read-only. */
  protected readonly editForm = inject(FormBuilder).nonNullable.group({ contactNumber: '', address: '' });

  ngOnInit(): void {
    this.validation.rules('patient').subscribe((rules) => this.validation.apply(this.editForm, rules));
    this.query.valueChanges
      .pipe(
        startWith(''),
        debounceTime(300),
        distinctUntilChanged(),
        tap(() => this.page.set(0)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.load());
    const openId = Number(this.open());
    if (openId) this.view(openId);
  }

  protected load(): void {
    this.loading.set(true);
    this.api
      .searchPatients(this.query.value.trim(), this.page())
      .pipe(catchError(() => of(null)))
      .subscribe((r) => {
        this.result.set(r);
        this.loading.set(false);
      });
  }

  protected go(delta: number): void {
    this.page.update((p) => Math.max(0, p + delta));
    this.load();
  }

  protected view(id: number): void {
    this.editError.set(null);
    this.api.patient(id).subscribe({
      next: (p) => {
        this.selected.set(p);
        this.editForm.reset({ contactNumber: p.contactNumber, address: p.address ?? '' });
      },
      error: (p: Problem) => this.toast.error(p.detail),
    });
  }

  protected save(): void {
    const p = this.selected();
    if (!p || !touchAll(this.editForm)) return;
    this.saving.set(true);
    const v = this.editForm.getRawValue();
    this.api.updateContact(p.id, v.contactNumber.trim(), v.address.trim() || null).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.selected.set(updated);
        this.editForm.reset({ contactNumber: updated.contactNumber, address: updated.address ?? '' });
        this.toast.success('Patient contact details updated.');
        this.load();
      },
      error: (e: Problem) => {
        this.saving.set(false);
        if (!applyServerErrors(this.editForm, e.errors)) this.editError.set(e.detail);
      },
    });
  }
}
