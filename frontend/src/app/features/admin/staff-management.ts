import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { FormRules, Problem, StaffAccount } from '../../core/models';
import { AdminApi } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { ValidationService, applyServerErrors, touchAll } from '../../core/services/validation.service';
import { FieldError } from '../../shared/field-error';
import { Modal } from '../../shared/modal';

type Mode = 'create' | 'edit' | 'password' | 'username' | 'delete';

@Component({
  selector: 'app-staff-management',
  imports: [ReactiveFormsModule, DatePipe, Modal, FieldError],
  templateUrl: './staff-management.html',
})
export class StaffManagement implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly validation = inject(ValidationService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly staff = signal<StaffAccount[]>([]);
  protected readonly loading = signal(true);
  protected readonly mode = signal<Mode | null>(null);
  protected readonly target = signal<StaffAccount | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  private rules: FormRules = {};

  protected readonly createForm = this.fb.group({ username: '', password: '', fullName: '', email: '', phoneNumber: '' });
  protected readonly editForm = this.fb.group({ fullName: '', email: '', phoneNumber: '', enabled: true });
  protected readonly passwordForm = this.fb.group({ newPassword: '' });
  protected readonly usernameForm = this.fb.group({ newUsername: '' });

  ngOnInit(): void {
    this.validation.rules('staff').subscribe((rules) => {
      this.rules = rules;
      this.validation.apply(this.createForm, rules);
      this.validation.apply(this.editForm, rules);
      this.validation.apply(this.passwordForm, rules, { newPassword: 'password' });
      this.validation.apply(this.usernameForm, rules, { newUsername: 'username' });
    });
    this.load();
  }

  protected load(): void {
    this.api.staff().subscribe({
      next: (s) => {
        this.staff.set(s);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected openModal(mode: Mode, s: StaffAccount | null = null): void {
    this.error.set(null);
    this.target.set(s);
    this.createForm.reset();
    this.passwordForm.reset();
    this.usernameForm.reset({ newUsername: s?.username ?? '' });
    if (s) this.editForm.reset({ fullName: s.fullName, email: s.email ?? '', phoneNumber: s.phoneNumber ?? '', enabled: s.enabled });
    this.mode.set(mode);
  }

  protected close(): void {
    this.mode.set(null);
    this.target.set(null);
  }

  protected modalTitle(): string {
    const name = this.target()?.fullName ?? '';
    switch (this.mode()) {
      case 'create': return 'Add staff account';
      case 'edit': return `Edit ${name}`;
      case 'password': return `Reset password — ${name}`;
      case 'username': return `Reset username — ${name}`;
      case 'delete': return 'Delete staff account';
      default: return '';
    }
  }

  protected submit(): void {
    const mode = this.mode();
    const s = this.target();
    this.error.set(null);
    switch (mode) {
      case 'create':
        return this.run(this.createForm, () => this.api.createStaff(this.blankToNull(this.createForm.getRawValue())), 'Staff account created.');
      case 'edit':
        return this.run(this.editForm, () => this.api.updateStaff(s!.id, this.blankToNull(this.editForm.getRawValue())), 'Staff account updated.');
      case 'password':
        return this.run(this.passwordForm, () => this.api.resetPassword(s!.id, this.passwordForm.getRawValue().newPassword),
          'Password reset. The user has been signed out everywhere.');
      case 'username':
        return this.run(this.usernameForm, () => this.api.resetUsername(s!.id, this.usernameForm.getRawValue().newUsername.trim()),
          'Username changed. The user has been signed out everywhere.');
      case 'delete':
        return this.run(null, () => this.api.deleteStaff(s!.id), 'Staff account deleted.');
    }
  }

  private run(form: FormGroup | null, call: () => import('rxjs').Observable<unknown>, success: string): void {
    if (form && !touchAll(form)) return;
    this.busy.set(true);
    call().subscribe({
      next: () => {
        this.busy.set(false);
        this.toast.success(success);
        this.close();
        this.load();
      },
      error: (p: Problem) => {
        this.busy.set(false);
        if (!form || !applyServerErrors(form, p.errors)) this.error.set(p.detail);
      },
    });
  }

  private blankToNull<T extends Record<string, unknown>>(v: T): T {
    return Object.fromEntries(Object.entries(v).map(([k, x]) => [k, x === '' ? null : x])) as T;
  }
}
