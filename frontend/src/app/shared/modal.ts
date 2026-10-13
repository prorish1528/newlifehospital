import { Component, ElementRef, effect, input, output, viewChild } from '@angular/core';

/** Accessible modal built on the native <dialog> element (focus trapping and Esc handling for free). */
@Component({
  selector: 'app-modal',
  template: `
    <dialog #dlg class="modal" [class.modal-wide]="wide()" (close)="closed.emit()" (cancel)="closed.emit()">
      <header class="modal-header">
        <h2>{{ title() }}</h2>
        <button type="button" class="icon-btn" (click)="closed.emit()" aria-label="Close">×</button>
      </header>
      <div class="modal-body"><ng-content /></div>
    </dialog>
  `,
})
export class Modal {
  readonly title = input.required<string>();
  readonly open = input(false);
  readonly wide = input(false);
  readonly closed = output<void>();
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dlg');

  constructor() {
    effect(() => {
      const el = this.dialog().nativeElement;
      if (this.open() && !el.open) el.showModal();
      if (!this.open() && el.open) el.close();
    });
  }
}
