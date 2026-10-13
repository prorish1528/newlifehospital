import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { CmsEntry, FieldRule, Problem } from '../../core/models';
import { AdminApi } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { ValidationService } from '../../core/services/validation.service';
import { FieldError } from '../../shared/field-error';

interface Row {
  entry: CmsEntry;
  control: FormControl<string>;
  saving: boolean;
}

@Component({
  selector: 'app-cms-admin',
  imports: [ReactiveFormsModule, DatePipe, FieldError],
  template: `
    <div class="page-title">
      <h1>Website Content</h1>
      <p class="muted">Edit the text shown on the public website. Plain text only; changes are live immediately.</p>
    </div>
    <div class="stack">
      @for (r of rows(); track r.entry.sectionKey) {
        <section class="card cms-row">
          <div class="flex-between">
            <label [for]="'cms-' + r.entry.sectionKey"><strong>{{ r.entry.label }}</strong> <code class="muted">{{ r.entry.sectionKey }}</code></label>
            <small class="muted">Updated {{ r.entry.updatedAt | date: 'dd MMM yyyy, h:mm a' }}@if (r.entry.updatedBy) { by {{ r.entry.updatedBy }}}</small>
          </div>
          <textarea [id]="'cms-' + r.entry.sectionKey" [formControl]="r.control" [rows]="r.entry.contentText.length > 120 ? 4 : 2"></textarea>
          <app-field-error [control]="r.control" />
          <div class="form-actions">
            <button type="button" class="btn btn-ghost btn-sm" [disabled]="r.control.pristine" (click)="r.control.reset(r.entry.contentText)">Revert</button>
            <button type="button" class="btn btn-primary btn-sm" [disabled]="r.control.pristine || r.saving" (click)="save(r)">Save</button>
          </div>
        </section>
      }
    </div>
  `,
})
export class CmsAdmin implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly validation = inject(ValidationService);
  private readonly toast = inject(ToastService);
  protected readonly rows = signal<Row[]>([]);
  private rule?: FieldRule;

  ngOnInit(): void {
    this.validation.rules('cms').subscribe((r) => {
      this.rule = r['contentText'];
      this.api.cms().subscribe((entries) =>
        this.rows.set(
          entries.map((entry) => {
            const control = new FormControl(entry.contentText, { nonNullable: true });
            if (this.rule) this.validation.applyRule(control, this.rule);
            return { entry, control, saving: false };
          }),
        ),
      );
    });
  }

  protected save(row: Row): void {
    row.control.markAsTouched();
    if (row.control.invalid) return;
    this.patch(row, { saving: true });
    this.api.updateCms(row.entry.sectionKey, row.control.value.trim()).subscribe({
      next: (entry) => {
        row.control.reset(entry.contentText);
        this.patch(row, { entry, saving: false });
        this.toast.success(`“${entry.label}” saved.`);
      },
      error: (p: Problem) => {
        this.patch(row, { saving: false });
        row.control.setErrors({ server: p.errors?.['contentText'] ?? p.detail });
      },
    });
  }

  private patch(row: Row, changes: Partial<Row>): void {
    Object.assign(row, changes);
    this.rows.set([...this.rows()]);
  }
}
