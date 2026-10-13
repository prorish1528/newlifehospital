import { inject } from '@angular/core';
import { CanActivateChildFn, CanMatchFn, Router } from '@angular/router';
import { Role } from '../models';
import { AuthStore } from '../services/auth.store';

/**
 * Route-level role check. This is a UX convenience only: the server enforces the same rule on every request
 * with URL rules and `@PreAuthorize`.
 */
export function roleGuard(role: Role): CanMatchFn {
  return (_route, segments) => {
    const auth = inject(AuthStore);
    const router = inject(Router);
    if (auth.role() === role) {
      return true;
    }
    const returnUrl = '/' + segments.map((s) => s.path).join('/');
    return router.createUrlTree(['/sign-in', role === 'ADMIN' ? 'admin' : 'staff'], {
      queryParams: { returnUrl },
    });
  };
}

/** Sends an already signed-in user from the sign-in pages straight to their portal. */
export const guestGuard: CanActivateChildFn = () => {
  const auth = inject(AuthStore);
  const role = auth.role();
  return role ? inject(Router).createUrlTree([auth.homeFor(role)]) : true;
};
