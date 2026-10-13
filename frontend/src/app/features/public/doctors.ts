import { Component, inject, signal } from '@angular/core';
import { Doctor } from '../../core/models';
import { PublicApi } from '../../core/services/api.service';
import { DoctorAvatar } from '../../shared/doctor-avatar';

@Component({
  selector: 'app-doctors',
  imports: [DoctorAvatar],
  template: `
    <section class="page-head">
      <div class="container">
        <h1>Our Doctors</h1>
        <p class="lead-dark">Experienced consultants across specialities, available for OPD and inpatient care.</p>
      </div>
    </section>
    <section class="container section">
      @if (loading()) {
        <div class="grid-cards">@for (i of [1, 2, 3, 4]; track i) {<div class="card skeleton" style="height: 260px"></div>}</div>
      } @else if (error()) {
        <div class="alert alert-error">Doctor profiles could not be loaded. Please try again later.</div>
      } @else {
        <div class="grid-cards">
          @for (d of doctors(); track d.id) {
            <article class="card doctor-card">
              <app-doctor-avatar [doctor]="d" size="lg" />
              <h3>{{ d.name }}</h3>
              <p class="specialty">{{ d.specialization }}</p>
              @if (d.qualification) {<p class="muted">{{ d.qualification }}</p>}
              @if (d.experienceYears != null) {<p class="pill">{{ d.experienceYears }}+ years experience</p>}
            </article>
          } @empty {
            <p class="muted">Doctor profiles will be published soon.</p>
          }
        </div>
      }
    </section>
  `,
})
export class Doctors {
  protected readonly doctors = signal<Doctor[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);

  constructor() {
    inject(PublicApi).doctors().subscribe({
      next: (d) => {
        this.doctors.set(d);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
