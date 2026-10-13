import { Component, inject, input, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthStore } from '../core/services/auth.store';
import { Logo } from './logo';

export interface NavItem {
  path: string;
  label: string;
  icon: string;
}

/** Shell for the Staff and Admin portals. Nav items arrive through route data (component input binding). */
@Component({
  selector: 'app-portal-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Logo],
  template: `
    <div class="portal" [class.nav-open]="navOpen()">
      <aside class="sidebar" aria-label="Portal navigation">
        <a routerLink="/" class="sidebar-brand"><app-logo /><span>New Life<br /><small>{{ portalName() }}</small></span></a>
        <nav>
          @for (item of nav(); track item.path) {
            <a [routerLink]="item.path" routerLinkActive="active" (click)="navOpen.set(false)">
              <span class="nav-icon" aria-hidden="true">{{ item.icon }}</span>{{ item.label }}
            </a>
          }
        </nav>
      </aside>
      <div class="portal-main">
        <header class="portal-header">
          <button type="button" class="icon-btn menu-btn" (click)="navOpen.set(!navOpen())" aria-label="Toggle navigation">☰</button>
          <div class="portal-title">{{ portalName() }}</div>
          @if (auth.user(); as u) {
            <div class="user-chip">
              <span class="avatar-sm" aria-hidden="true">{{ u.fullName.charAt(0) }}</span>
              <span class="user-meta"><strong>{{ u.fullName }}</strong><small>{{ u.username }} · {{ u.role }}</small></span>
              <a class="btn btn-sm btn-ghost" [routerLink]="accountPath()">Account</a>
              <button type="button" class="btn btn-sm btn-outline" (click)="auth.logout()">Sign out</button>
            </div>
          }
        </header>
        <main class="portal-content"><router-outlet /></main>
      </div>
    </div>
  `,
})
export class PortalLayout {
  readonly nav = input<NavItem[]>([]);
  readonly portalName = input('Portal');
  readonly accountPath = input('account');
  protected readonly auth = inject(AuthStore);
  protected readonly navOpen = signal(false);
}
