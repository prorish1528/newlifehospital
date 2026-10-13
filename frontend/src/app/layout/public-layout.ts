import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { PublicApi } from '../core/services/api.service';
import { AuthStore } from '../core/services/auth.store';
import { Logo } from './logo';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Logo],
  template: `
    <a class="skip-link" href="#main">Skip to content</a>
    <header class="site-header">
      <div class="container header-row">
        <a routerLink="/" class="brand" aria-label="New Life Hospital home">
          <app-logo />
          <span class="brand-text">
            <span class="brand-name">New Life Hospital</span>
            <span class="brand-sub">Indore - Ujjain Rd</span>
          </span>
        </a>
        <nav class="tabs" aria-label="Website sections">
          <a routerLink="/about" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: false }">About Hospital</a>
          <a routerLink="/doctors" routerLinkActive="active">Doctors</a>
          <a routerLink="/services" routerLinkActive="active">List of Services</a>
        </nav>
        @if (auth.user(); as u) {
          <a class="btn btn-primary" [routerLink]="auth.homeFor(u.role)">My Portal</a>
        } @else {
          <a class="btn btn-primary" routerLink="/sign-in">Sign In</a>
        }
      </div>
      <div class="emergency-strip">
        <div class="container">
          <strong>24x7 Emergency:</strong> {{ cms()['emergency_phone'] }} &nbsp;·&nbsp; Ambulance: 108
        </div>
      </div>
    </header>

    <main id="main"><router-outlet /></main>

    <footer class="site-footer">
      <div class="container footer-grid">
        <div>
          <div class="footer-brand"><app-logo /> New Life Hospital</div>
          <p>{{ cms()['address'] }}</p>
        </div>
        <div>
          <h3>Contact</h3>
          <p>Reception: {{ cms()['contact_phone'] }}<br />Email: {{ cms()['contact_email'] }}</p>
        </div>
        <div>
          <h3>Hours</h3>
          <p>{{ cms()['visiting_hours'] }}</p>
        </div>
      </div>
      <div class="container footer-bottom">© {{ year }} New Life Hospital, Indore - Ujjain Rd. All rights reserved.</div>
    </footer>
  `,
})
export class PublicLayout {
  protected readonly auth = inject(AuthStore);
  protected readonly cms = toSignal(inject(PublicApi).cms(), {
    initialValue: {} as Record<string, string>,
  });
  protected readonly year = new Date().getFullYear();
}
