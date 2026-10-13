import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-sign-in-chooser',
  imports: [RouterLink],
  template: `
    <section class="auth-wrap">
      <div class="container">
        <h1 class="center">Sign in to New Life Hospital</h1>
        <p class="center muted">Choose your portal. Accounts are issued by the hospital administration.</p>
        <div class="portal-choices">
          <a class="card portal-card" routerLink="/sign-in/staff">
            <span class="portal-icon" aria-hidden="true">🩺</span>
            <h2>Staff Portal</h2>
            <p>Patient registration, search, and billing &amp; invoices.</p>
            <span class="btn btn-primary">Continue as Staff</span>
          </a>
          <a class="card portal-card" routerLink="/sign-in/admin">
            <span class="portal-icon" aria-hidden="true">🛡️</span>
            <h2>Admin Portal</h2>
            <p>Staff accounts, charges sheet, doctors, website content and revenue dashboard.</p>
            <span class="btn btn-dark">Continue as Admin</span>
          </a>
        </div>
      </div>
    </section>
  `,
})
export class SignInChooser {}
