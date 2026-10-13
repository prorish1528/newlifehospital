import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl } from '@angular/forms';
import { merge } from 'rxjs';

/**
 * Shows the first error of a control once it is touched. Messages come from the database rule
 * (`rule`), a server response (`server`) or a component-level check (`custom`).
 */
@Component({
  selector: 'app-field-error',
  template: `@if (message(); as m) {<div class="field-error" role="alert">{{ m }}</div>}`,
})
export class FieldError implements OnInit {
  readonly control = input.required<AbstractControl>();
  private readonly destroyRef = inject(DestroyRef);
  protected readonly message = signal<string | null>(null);

  ngOnInit(): void {
    const c = this.control();
    merge(c.statusChanges, c.events)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.message.set(this.compute(c)));
    this.message.set(this.compute(c));
  }

  private compute(c: AbstractControl): string | null {
    if (!c.errors || !(c.touched || c.dirty)) return null;
    const e = c.errors;
    return (e['server'] ?? e['custom'] ?? e['duplicate'] ?? e['required'] ?? e['rule'] ?? null) as string | null;
  }
}
