import { Component, computed, input, signal } from '@angular/core';
import { Doctor } from '../core/models';
import { initials } from './util';

@Component({
  selector: 'app-doctor-avatar',
  template: `
    @if (doctor().photoUrl && !failed()) {
      <img [src]="doctor().photoUrl" [alt]="'Photo of ' + doctor().name" [class]="'avatar avatar-' + size()"
           loading="lazy" (error)="failed.set(true)" />
    } @else {
      <div [class]="'avatar avatar-initials avatar-' + size()" aria-hidden="true">{{ text() }}</div>
    }
  `,
})
export class DoctorAvatar {
  readonly doctor = input.required<Doctor>();
  readonly size = input<'sm' | 'lg'>('sm');
  protected readonly failed = signal(false);
  protected readonly text = computed(() => initials(this.doctor().name));
}
