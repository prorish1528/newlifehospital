import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Problem, Role } from '../../core/models';
import { AuthStore } from '../../core/services/auth.store';
import { ValidationService, touchAll } from '../../core/services/validation.service';
import { FieldError } from '../../shared/field-error';
import { safeReturnUrl } from '../../shared/util';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, FieldError],
  template: `
    <section class="auth-wrap">
      <div class="card auth-card">
        <div class="auth-badge" [class.auth-badge-admin]="portal() === 'ADMIN'">{{ title() }}</div>
        <h1>Sign in</h1>
        @if (expired()) {<div class="alert alert-info">Your session ended. Please sign in again.</div>}
        @if (signedOut()) {<div class="alert alert-success">You have been signed out.</div>}
        @if (error(); as e) {<div class="alert alert-error" role="alert">{{ e }}</div>}

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div class="field">
            <label for="username">Username</label>
            <input id="username" formControlName="username" autocomplete="username" autocapitalize="none" spellcheck="false" />
            <app-field-error [control]="form.controls.username" />
          </div>
          <div class="field">
            <label for="password">Password</label>
            <div class="input-suffix">
              <input id="password" [type]="showPassword() ? 'text' : 'password'" formControlName="password" autocomplete="current-password" />
              <button type="button" class="btn btn-sm btn-ghost" (click)="showPassword.set(!showPassword())"
                      [attr.aria-pressed]="showPassword()">{{ showPassword() ? 'Hide' : 'Show' }}</button>
            </div>
            <app-field-error [control]="form.controls.password" />
          </div>
          <button type="submit" class="btn btn-primary btn-block" [disabled]="busy()">
            {{ busy() ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>
        <p class="muted small center mt-2">
          @if (portal() === 'ADMIN') {
            Staff member? <a routerLink="/sign-in/staff">Use the Staff Portal</a>
          } @else {
            Administrator? <a routerLink="/sign-in/admin">Use the Admin Portal</a>
          }
          · Forgot your password? Contact the hospital administrator.
        </p>
      </div>
    </section>
  `,
})
export class Login implements OnInit {
  /** Bound from route data / query params via component input binding. */
  readonly portal = input<Role>('STAFF');
  readonly returnUrl = input<string>();
  readonly expired = input<string>();
  readonly signedOut = input<string>();

  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly validation = inject(ValidationService);

  protected readonly form = inject(FormBuilder).nonNullable.group({ username: '', password: '' });
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showPassword = signal(false);
  protected readonly title = computed(() => (this.portal() === 'ADMIN' ? 'Admin Portal' : 'Staff Portal'));

  ngOnInit(): void {
    this.validation.rules('login').subscribe((rules) => this.validation.apply(this.form, rules));
  }

  protected submit(): void {
    this.error.set(null);
    if (!touchAll(this.form)) return;
    this.busy.set(true);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username.trim(), password, this.portal()).subscribe({
      next: (user) => {
        this.busy.set(false);
        this.router.navigateByUrl(safeReturnUrl(this.returnUrl(), this.auth.homeFor(user.role)));
      },
      error: (p: Problem) => {
        this.busy.set(false);
        this.form.controls.password.reset();
        this.error.set(p.status === 400 ? 'Enter your username and password.' : p.detail);
      },
    });
  }
}
