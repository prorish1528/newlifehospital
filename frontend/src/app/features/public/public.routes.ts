import { Routes } from '@angular/router';

export const PUBLIC_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'about' },
  { path: 'about', title: 'About | New Life Hospital', loadComponent: () => import('./about').then((m) => m.About) },
  { path: 'doctors', title: 'Doctors | New Life Hospital', loadComponent: () => import('./doctors').then((m) => m.Doctors) },
  { path: 'services', title: 'Services | New Life Hospital', loadComponent: () => import('./services').then((m) => m.Services) },
];
