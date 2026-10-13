import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, firstValueFrom, of, tap } from 'rxjs';
import { Role, SessionUser } from '../models';

/**
 * Session state. The JWT itself lives in an HttpOnly cookie that scripts cannot read; this store only keeps
 * the non-sensitive profile returned by the server.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly _user = signal<SessionUser | null>(null);
  private expiryTimer?: ReturnType<typeof setTimeout>;

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly role = computed(() => this._user()?.role ?? null);

  /** Called once at start-up; also obtains the XSRF-TOKEN cookie. Never throws. */
  restore(): Promise<unknown> {
    return firstValueFrom(
      this.http.get<SessionUser | null>('/api/auth/me').pipe(
        tap((u) => this.set(u ?? null)),
        catchError(() => of(null)),
      ),
    );
  }

  login(username: string, password: string, portal: Role): Observable<SessionUser> {
    return this.http
      .post<SessionUser>('/api/auth/login', { username, password, portal })
      .pipe(tap((u) => this.set(u)));
  }

  changePassword(currentPassword: string, newPassword: string): Observable<SessionUser> {
    return this.http
      .post<SessionUser>('/api/auth/change-password', { currentPassword, newPassword })
      .pipe(tap((u) => this.set(u)));
  }

  logout(): void {
    const role = this.role();
    this.http.post('/api/auth/logout', {}).pipe(catchError(() => of(null))).subscribe(() => {
      this.clear();
      this.router.navigate(['/sign-in', role === 'ADMIN' ? 'admin' : 'staff'], {
        queryParams: { signedOut: 1 },
      });
    });
  }

  /** Local-only reset, used when the server reports the session is gone. */
  clear(): void {
    clearTimeout(this.expiryTimer);
    this._user.set(null);
  }

  homeFor(role: Role): string {
    return role === 'ADMIN' ? '/admin' : '/staff';
  }

  private set(user: SessionUser | null): void {
    clearTimeout(this.expiryTimer);
    this._user.set(user);
    if (user?.expiresAt) {
      const ms = new Date(user.expiresAt).getTime() - Date.now();
      if (ms > 0 && ms < 2 ** 31) {
        this.expiryTimer = setTimeout(() => {
          this.clear();
          this.router.navigate(['/sign-in', user.role === 'ADMIN' ? 'admin' : 'staff'], {
            queryParams: { expired: 1 },
          });
        }, ms);
      }
    }
  }
}
