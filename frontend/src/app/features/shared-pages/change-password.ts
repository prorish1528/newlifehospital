import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { Problem } from '../../core/models';
import { AuthStore } from '../../core/services/auth.store';
import { ToastService } from '../../core/services/toast.service';
import { ValidationService, applyServerErrors, touchAll } from '../../core/services/validation.service';
import { FieldError } from '../../shared/field-error';

@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, FieldError],
  template: `
    <div class="page-title"><h1>My Account</h1></div>
    <div class="card narrow">
      <dl class="detail-grid">
        <div><dt>Name</dt><dd>{{ auth.user()?.fullName }}</dd></div>
        <div><dt>Username</dt><dd>{{ auth.user()?.username }}</dd></div>
        <div><dt>Role</dt><dd>{{ auth.user()?.role }}</dd></div>
      </dl>
      <h2 class="card-title">Change password</h2>
      <p class="muted small">Changing your password signs you out on every other device.</p>
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        @if (error(); as e) {<div class="alert alert-error">{{ e }}</div>}
        <div class="field">
          <label for="cur">Current password</label>
          <input id="cur" type="password" formControlName="currentPassword" autocomplete="current-password" />
          <app-field-error [control]="form.controls.currentPassword" />
        </div>
        <div class="field">
          <label for="new">New password</label>
          <input id="new" type="password" formControlName="newPassword" autocomplete="new-password" />
          <app-field-error [control]="form.controls.newPassword" />
        </div>
        <div class="field">
          <label for="confirm">Confirm new password</label>
          <input id="confirm" type="password" formControlName="confirm" autocomplete="new-password" />
          <app-field-error [control]="form.controls.confirm" />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Change password' }}</button>
        </div>
      </form>
    </div>
  `,
})
export class ChangePassword implements OnInit {
  protected readonly auth = inject(AuthStore);
  private readonly validation = inject(ValidationService);
  private readonly toast = inject(ToastService);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = inject(FormBuilder).nonNullable.group({ currentPassword: '', newPassword: '', confirm: '' });

  ngOnInit(): void {
    this.form.controls.currentPassword.setValidators((c) => (c.value ? null : { required: 'Current password is required.' }));
    this.form.controls.confirm.setValidators((c: AbstractControl): ValidationErrors | null =>
      c.value === this.form.controls.newPassword.value ? null : { custom: 'Passwords do not match.' },
    );
    this.validation.rules('staff').subscribe((rules) => this.validation.applyRule(this.form.controls.newPassword, rules['password']));
  }

  protected submit(): void {
    this.error.set(null);
    this.form.controls.confirm.updateValueAndValidity();
    if (!touchAll(this.form)) return;
    this.busy.set(true);
    const v = this.form.getRawValue();
    this.auth.changePassword(v.currentPassword, v.newPassword).subscribe({
      next: () => {
        this.busy.set(false);
        this.form.reset();
        this.toast.success('Password changed.');
      },
      error: (p: Problem) => {
        this.busy.set(false);
        if (!applyServerErrors(this.form, p.errors)) this.error.set(p.detail);
      },
    });
  }
}
