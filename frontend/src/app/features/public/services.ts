import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { HospitalService } from '../../core/models';
import { PublicApi } from '../../core/services/api.service';

@Component({
  selector: 'app-services',
  imports: [CurrencyPipe],
  template: `
    <section class="page-head">
      <div class="container">
        <h1>List of Services</h1>
        <p class="lead-dark">Consultations, diagnostics, procedures, inpatient care and emergency services.</p>
      </div>
    </section>
    <section class="container section">
      <div class="toolbar">
        <input type="search" class="search-input" placeholder="Search services…" aria-label="Search services"
               [value]="query()" (input)="query.set($any($event.target).value)" />
        <div class="chip-row" role="tablist" aria-label="Filter by category">
          <button type="button" class="chip" [class.active]="category() === ''" (click)="category.set('')">All</button>
          @for (cat of categories(); track cat) {
            <button type="button" class="chip" [class.active]="category() === cat" (click)="category.set(cat)">{{ cat }}</button>
          }
        </div>
      </div>

      @if (loading()) {
        <p class="muted">Loading services…</p>
      } @else if (error()) {
        <div class="alert alert-error">Services could not be loaded. Please try again later.</div>
      } @else {
        @for (group of grouped(); track group.category) {
          <h2 class="group-title">{{ group.category }}</h2>
          <div class="grid-services">
            @for (s of group.items; track s.id) {
              <article class="card service-card">
                <div class="service-head">
                  <h3>{{ s.serviceName }}</h3>
                  <span class="price">{{ s.cost | currency: 'INR' : 'symbol' : '1.0-2' : 'en-IN' }}</span>
                </div>
                @if (s.description) {<p class="muted">{{ s.description }}</p>}
              </article>
            }
          </div>
        } @empty {
          <p class="muted">No services match your search.</p>
        }
        <p class="muted small mt-2">Indicative charges; final billing depends on clinical requirements.</p>
      }
    </section>
  `,
})
export class Services {
  private readonly all = signal<HospitalService[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly query = signal('');
  protected readonly category = signal('');

  protected readonly categories = computed(() => [...new Set(this.all().map((s) => s.category))].sort());
  protected readonly grouped = computed(() => {
    const q = this.query().trim().toLowerCase();
    const cat = this.category();
    const filtered = this.all().filter(
      (s) =>
        (!cat || s.category === cat) &&
        (!q || s.serviceName.toLowerCase().includes(q) || (s.description ?? '').toLowerCase().includes(q)),
    );
    const map = new Map<string, HospitalService[]>();
    for (const s of filtered) map.set(s.category, [...(map.get(s.category) ?? []), s]);
    return [...map.entries()].map(([category, items]) => ({ category, items }));
  });

  constructor() {
    inject(PublicApi).services().subscribe({
      next: (s) => {
        this.all.set(s);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
