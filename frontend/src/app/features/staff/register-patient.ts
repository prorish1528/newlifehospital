import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PatientDetail, PatientSummary } from '../../core/models';
import { PatientRegisterForm } from '../../shared/patient-register-form';

@Component({
  selector: 'app-register-patient',
  imports: [PatientRegisterForm, RouterLink],
  template: `
    <div class="page-title">
      <h1>Patient Registration</h1>
      <p class="muted">All fields are validated against hospital rules. Existing patients are suggested as you type.</p>
    </div>

    @if (last(); as p) {
      <div class="alert alert-success flex-between">
        <span><strong>{{ p.name }}</strong> registered as <strong>{{ p.patientCode }}</strong> (age {{ p.age }}).</span>
        <span class="btn-row">
          <a class="btn btn-sm btn-primary" routerLink="/staff/billing" [queryParams]="{ patientId: p.id }">Create invoice</a>
          <button type="button" class="btn btn-sm btn-ghost" (click)="last.set(null)">Dismiss</button>
        </span>
      </div>
    }

    <div class="card">
      <app-patient-register-form (registered)="last.set($event)" (useExisting)="openExisting($event)" />
    </div>
  `,
})
export class RegisterPatient {
  private readonly router = inject(Router);
  protected readonly last = signal<PatientDetail | null>(null);

  protected openExisting(p: PatientSummary): void {
    this.router.navigate(['/staff/patients'], { queryParams: { open: p.id } });
  }
}
