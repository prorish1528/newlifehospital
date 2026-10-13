import { Routes } from '@angular/router';
import { NavItem, PortalLayout } from '../../layout/portal-layout';

const NAV: NavItem[] = [
  { path: '/admin/dashboard', label: 'Dashboard', icon: '▤' },
  { path: '/admin/invoices', label: 'Invoices', icon: '☰' },
  { path: '/admin/staff', label: 'Staff Management', icon: '👥' },
  { path: '/admin/charges', label: 'Charges Sheet', icon: '₹' },
  { path: '/admin/doctors', label: 'Doctors', icon: '⚕' },
  { path: '/admin/cms', label: 'Website Content', icon: '✎' },
];

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: PortalLayout,
    data: { nav: NAV, portalName: 'Admin Portal', accountPath: '/admin/account' },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', title: 'Dashboard | Admin', loadComponent: () => import('./dashboard').then((m) => m.AdminDashboard) },
      { path: 'staff', title: 'Staff | Admin', loadComponent: () => import('./staff-management').then((m) => m.StaffManagement) },
      { path: 'charges', title: 'Charges | Admin', loadComponent: () => import('./charges').then((m) => m.Charges) },
      { path: 'doctors', title: 'Doctors | Admin', loadComponent: () => import('./doctors-admin').then((m) => m.DoctorsAdmin) },
      { path: 'cms', title: 'Website Content | Admin', loadComponent: () => import('./cms-admin').then((m) => m.CmsAdmin) },
      {
        path: 'invoices',
        title: 'Invoices | Admin',
        data: { scope: 'admin' },
        loadComponent: () => import('../shared-pages/invoice-list').then((m) => m.InvoiceList),
      },
      {
        path: 'invoices/:id',
        title: 'Invoice | Admin',
        data: { scope: 'admin' },
        loadComponent: () => import('../shared-pages/invoice-view').then((m) => m.InvoiceView),
      },
      {
        path: 'account',
        title: 'Account | Admin',
        loadComponent: () => import('../shared-pages/change-password').then((m) => m.ChangePassword),
      },
    ],
  },
];
