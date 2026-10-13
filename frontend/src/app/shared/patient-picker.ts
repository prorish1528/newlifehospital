import { Component, DestroyRef, OnInit, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';
import { GENDER_LABELS, PatientSummary } from '../core/models';
import { StaffApi } from '../core/services/api.service';

/** Autocomplete patient lookup by name, patient code, contact number or exact ID number. */
@Component({
  selector: 'app-patient-picker',
  imports: [ReactiveFormsModule],
  template: `
    <div class="suggest-anchor">
      <label class="sr-only" for="picker">Search patient</label>
      <input id="picker" type="search" [formControl]="query" autocomplete="off" class="search-input"
             [placeholder]="placeholder()" (keydown.escape)="results.set([])" />
      @if (searching()) {
        <div class="suggest-panel"><div class="suggest-title">Searching…</div></div>
      } @else if (results().length) {
        <div class="suggest-panel" role="listbox">
          @for (p of results(); track p.id) {
            <button type="button" class="suggest-item" role="option" (click)="pick(p)">
              <strong>{{ p.name }}</strong>
              <span>{{ p.patientCode }} · {{ p.age }} yrs · {{ genderLabels[p.gender] }} ·{{ p.contactNumber }} · ID {{ p.maskedIdNumber }}</span>
            </button>
          }
        </div>
      } @else if (noMatch()) {
        <div class="suggest-panel">
          <div class="suggest-empty">
            <span>No patient matches “{{ query.value }}”.</span>
            <button type="button" class="btn btn-sm btn-primary" (click)="registerRequested.emit()">Register new patient</button>
          </div>
        </div>
      }
    </div>
  `,
})
export class PatientPicker implements OnInit {
  readonly placeholder = input('Search by name, patient code, mobile or ID number');
  readonly selected = output<PatientSummary>();
  readonly registerRequested = output<void>();

  private readonly api = inject(StaffApi);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly genderLabels = GENDER_LABELS;
  protected readonly query = new FormControl('', { nonNullable: true });
  protected readonly results = signal<PatientSummary[]>([]);
  protected readonly searching = signal(false);
  protected readonly noMatch = signal(false);

  ngOnInit(): void {
    this.query.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap((q) => {
          this.noMatch.set(false);
          this.searching.set(q.trim().length >= 2);
        }),
        switchMap((q) => (q.trim().length >= 2 ? this.api.suggest(q.trim()).pipe(catchError(() => of([]))) : of([]))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((list) => {
        this.searching.set(false);
        this.results.set(list);
        this.noMatch.set(list.length === 0 && this.query.value.trim().length >= 2);
      });
  }

  protected pick(p: PatientSummary): void {
    this.selected.emit(p);
    this.clear();
  }

  clear(): void {
    this.query.setValue('', { emitEvent: false });
    this.results.set([]);
    this.noMatch.set(false);
  }
}
