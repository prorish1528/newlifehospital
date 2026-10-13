import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  { path: '', title: 'Sign In | New Life Hospital', loadComponent: () => import('./sign-in-chooser').then((m) => m.SignInChooser) },
  {
    path: 'staff',
    title: 'Staff Sign In | New Life Hospital',
    data: { portal: 'STAFF' },
    loadComponent: () => import('./login').then((m) => m.Login),
  },
  {
    path: 'admin',
    title: 'Admin Sign In | New Life Hospital',
    data: { portal: 'ADMIN' },
    loadComponent: () => import('./login').then((m) => m.Login),
  },
];
