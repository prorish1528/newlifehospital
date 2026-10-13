import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { PublicApi } from '../../core/services/api.service';

@Component({
  selector: 'app-about',
  imports: [RouterLink],
  template: `
    <section class="hero">
      <div class="container hero-inner">
        <div>
          <p class="eyebrow">Multi-speciality hospital · Indore - Ujjain Road</p>
          <h1>{{ c()['hero_title'] }}</h1>
          <p class="lead">{{ c()['hero_tagline'] }}</p>
          <div class="hero-actions">
            <a class="btn btn-light" routerLink="/doctors">Meet our doctors</a>
            <a class="btn btn-outline-light" routerLink="/services">View services</a>
          </div>
        </div>
        <div class="hero-card">
          <div class="hero-card-row"><span class="dot dot-live"></span> Emergency open 24x7</div>
          <div class="hero-card-big">{{ c()['emergency_phone'] }}</div>
          <div class="muted-light">or dial <strong>108</strong> for the national ambulance service</div>
        </div>
      </div>
    </section>

    <section class="container section">
      <div class="two-col">
        <div>
          <h2>About New Life Hospital</h2>
          <p class="prose">{{ c()['about_overview'] }}</p>
        </div>
        <div class="stack">
          <article class="card card-accent">
            <h3>Our Mission</h3>
            <p>{{ c()['mission'] }}</p>
          </article>
          <article class="card card-accent">
            <h3>Our Vision</h3>
            <p>{{ c()['vision'] }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="container section">
      <div class="emergency-panel">
        <div>
          <h2>Emergency Care</h2>
          <p>{{ c()['emergency_info'] }}</p>
        </div>
        <div class="emergency-numbers">
          <div><small>Emergency line</small><strong>{{ c()['emergency_phone'] }}</strong></div>
          <div><small>National ambulance</small><strong>108</strong></div>
        </div>
      </div>
    </section>

    <section class="container section">
      <h2>Location</h2>
      <div class="location">
        <figure class="map-card" aria-label="Map showing New Life Hospital on the Indore - Ujjain Road">
          <svg viewBox="0 0 600 320" role="img" aria-labelledby="map-title">
            <title id="map-title">Schematic map: New Life Hospital on Indore - Ujjain Road, between Indore and Ujjain</title>
            <rect width="600" height="320" fill="var(--map-land)" />
            <path d="M0 250 C120 230 160 300 300 270 S520 300 600 260" fill="none" stroke="var(--map-water)" stroke-width="14" opacity=".6" />
            <path d="M70 280 C170 220 260 190 330 150 S480 70 540 40" fill="none" stroke="var(--map-road-edge)" stroke-width="18" stroke-linecap="round" />
            <path d="M70 280 C170 220 260 190 330 150 S480 70 540 40" fill="none" stroke="var(--map-road)" stroke-width="12" stroke-linecap="round" />
            <path d="M70 280 C170 220 260 190 330 150 S480 70 540 40" fill="none" stroke="#fff" stroke-width="2" stroke-dasharray="10 10" />
            <circle cx="70" cy="280" r="9" fill="var(--ink)" /><text x="88" y="300" class="map-label">Indore</text>
            <circle cx="540" cy="40" r="9" fill="var(--ink)" /><text x="470" y="30" class="map-label">Ujjain</text>
            <text x="190" y="190" class="map-road-label" transform="rotate(-28 190 190)">Indore - Ujjain Rd</text>
            <g transform="translate(300 128)">
              <path d="M0 0 C-14 -16 -22 -26 -22 -38 A22 22 0 1 1 22 -38 C22 -26 14 -16 0 0Z" fill="var(--danger)" />
              <path d="M-4 -46h8v6h6v8h-6v6h-8v-6h-6v-8h6z" fill="#fff" />
            </g>
            <text x="330" y="112" class="map-label map-label-strong">New Life Hospital</text>
          </svg>
          <figcaption>Schematic only — use the map link for turn-by-turn directions.</figcaption>
        </figure>
        <div class="card">
          <h3>Address</h3>
          <p>{{ c()['address'] }}</p>
          <h3>Visiting Hours</h3>
          <p>{{ c()['visiting_hours'] }}</p>
          <h3>Reception</h3>
          <p>{{ c()['contact_phone'] }} · {{ c()['contact_email'] }}</p>
          <a class="btn btn-primary" [href]="mapUrl()" target="_blank" rel="noopener noreferrer">Open in Google Maps</a>
        </div>
      </div>
    </section>
  `,
})
export class About {
  protected readonly c = toSignal(inject(PublicApi).cms(), {
    initialValue: {} as Record<string, string>,
  });
  protected readonly mapUrl = computed(
    () =>
      'https://www.google.com/maps/search/?api=1&query=' +
      encodeURIComponent(this.c()['map_query'] ?? 'New Life Hospital Indore Ujjain Road'),
  );
}
