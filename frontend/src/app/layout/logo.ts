import { Component } from '@angular/core';

@Component({
  selector: 'app-logo',
  template: `
    <svg class="logo" viewBox="0 0 40 40" aria-hidden="true" focusable="false">
      <rect width="40" height="40" rx="10" fill="var(--brand)" />
      <path d="M16 9h8v7h7v8h-7v7h-8v-7H9v-8h7z" fill="#fff" />
      <path d="M12 20h5l2-4 3 8 2-4h4" fill="none" stroke="var(--brand)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
    </svg>
  `,
  styles: `:host { display: inline-flex; } .logo { width: 40px; height: 40px; }`,
})
export class Logo {}
