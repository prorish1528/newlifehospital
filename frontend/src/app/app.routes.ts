import { Routes } from '@angular/router';
import { guestGuard, roleGuard } from './core/guards/auth.guards';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/public-layout').then((m) => m.PublicLayout),
    loadChildren: () => import('./features/public/public.routes').then((m) => m.PUBLIC_ROUTES),
  },
  {
    path: 'sign-in',
    loadComponent: () => import('./layout/public-layout').then((m) => m.PublicLayout),
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
    canActivateChild: [guestGuard],
  },
  {
    path: 'staff',
    canMatch: [roleGuard('STAFF')],
    loadChildren: () => import('./features/staff/staff.routes').then((m) => m.STAFF_ROUTES),
  },
  {
    path: 'admin',
    canMatch: [roleGuard('ADMIN')],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
