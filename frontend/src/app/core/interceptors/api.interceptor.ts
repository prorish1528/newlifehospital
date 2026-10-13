import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Problem } from '../models';
import { AuthStore } from '../services/auth.store';
import { ToastService } from '../services/toast.service';

/**
 * Normalises API errors to {@link Problem}, handles expired sessions globally and reports network failures.
 * Same-origin `/api` requests carry the HttpOnly session cookie automatically.
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  const toast = inject(ToastService);
  const isAuthCall = req.url.startsWith('/api/auth/');

  return next(req).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse)) {
        return throwError(() => err);
      }
      const body = err.error && typeof err.error === 'object' ? (err.error as Partial<Problem>) : {};
      const problem: Problem = {
        status: err.status,
        detail: body.detail ?? defaultMessage(err.status),
        errors: body.errors,
      };
      if (err.status === 0 || err.status === 502 || err.status === 503 || err.status === 504) {
        toast.error('Cannot reach the server. Check your connection and try again.');
      } else if (err.status === 401 && !isAuthCall && auth.isAuthenticated()) {
        const role = auth.role();
        auth.clear();
        toast.info('Your session has expired. Please sign in again.');
        router.navigate(['/sign-in', role === 'ADMIN' ? 'admin' : 'staff'], {
          queryParams: { returnUrl: router.url, expired: 1 },
        });
      } else if (err.status === 403 && problem.detail.includes('permission')) {
        toast.error(problem.detail);
      } else if (err.status >= 500) {
        toast.error(problem.detail);
      }
      return throwError(() => problem);
    }),
  );
};

function defaultMessage(status: number): string {
  switch (status) {
    case 0:
      return 'Cannot reach the server.';
    case 401:
      return 'Please sign in.';
    case 403:
      return 'You do not have permission to perform this action.';
    case 404:
      return 'Not found.';
    case 409:
      return 'This conflicts with existing data.';
    case 429:
      return 'Too many attempts. Please wait and try again.';
    default:
      return 'Something went wrong. Please try again.';
  }
}
