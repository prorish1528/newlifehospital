import { Routes } from '@angular/router';
import { NavItem, PortalLayout } from '../../layout/portal-layout';

const NAV: NavItem[] = [
  { path: '/staff/register', label: 'Register Patient', icon: '＋' },
  { path: '/staff/patients', label: 'Patient Search & Edit', icon: '⌕' },
  { path: '/staff/billing', label: 'New Invoice', icon: '₹' },
  { path: '/staff/invoices', label: 'Invoices', icon: '☰' },
];

export const STAFF_ROUTES: Routes = [
  {
    path: '',
    component: PortalLayout,
    data: { nav: NAV, portalName: 'Staff Portal', accountPath: '/staff/account' },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'register' },
      {
        path: 'register',
        title: 'Register Patient | Staff',
        loadComponent: () => import('./register-patient').then((m) => m.RegisterPatient),
      },
      {
        path: 'patients',
        title: 'Patients | Staff',
        loadComponent: () => import('./patient-search').then((m) => m.PatientSearch),
      },
      {
        path: 'billing',
        title: 'New Invoice | Staff',
        loadComponent: () => import('./billing').then((m) => m.Billing),
      },
      {
        path: 'invoices',
        title: 'Invoices | Staff',
        data: { scope: 'staff' },
        loadComponent: () => import('../shared-pages/invoice-list').then((m) => m.InvoiceList),
      },
      {
        path: 'invoices/:id',
        title: 'Invoice | Staff',
        data: { scope: 'staff' },
        loadComponent: () => import('../shared-pages/invoice-view').then((m) => m.InvoiceView),
      },
      {
        path: 'account',
        title: 'Account | Staff',
        loadComponent: () => import('../shared-pages/change-password').then((m) => m.ChangePassword),
      },
    ],
  },
];
