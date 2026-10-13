import { CurrencyPipe } from '@angular/common';
import { Component, computed, input, signal } from '@angular/core';
import { RevenuePoint } from '../../core/models';

const W = 720;
const H = 240;
const PAD = { top: 12, right: 8, bottom: 28, left: 56 };

/**
 * Grouped bar chart of billed vs collected revenue on one INR axis. Hover tooltip per period,
 * legend, and a table view for screen readers / exact values.
 */
@Component({
  selector: 'app-revenue-chart',
  imports: [CurrencyPipe],
  template: `
    <figure class="viz">
      <div class="viz-head">
        <figcaption class="viz-title">{{ title() }}</figcaption>
        <div class="viz-legend" aria-hidden="true">
          <span><i class="swatch s1"></i>Billed</span>
          <span><i class="swatch s2"></i>Collected</span>
          <button type="button" class="link-btn" (click)="showTable.set(!showTable())">
            {{ showTable() ? 'Chart view' : 'Table view' }}</button>
        </div>
      </div>
      @if (showTable()) {
        <div class="table-wrap">
          <table class="table table-compact">
            <thead><tr><th>Period</th><th class="right">Invoices</th><th class="right">Billed</th><th class="right">Collected</th></tr></thead>
            <tbody>
              @for (p of points(); track p.label) {
                <tr><td>{{ fmt()(p.label) }}</td><td class="right">{{ p.invoiceCount }}</td>
                  <td class="right">{{ p.billed | currency: 'INR' }}</td><td class="right">{{ p.collected | currency: 'INR' }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      } @else {
        <div class="viz-plot" (mouseleave)="hover.set(-1)">
          <svg [attr.viewBox]="'0 0 ' + W + ' ' + H" role="img" [attr.aria-label]="title() + ': billed and collected revenue'">
            @for (t of ticks(); track t.v) {
              <line [attr.x1]="PAD.left" [attr.x2]="W - PAD.right" [attr.y1]="t.y" [attr.y2]="t.y" class="viz-grid" />
              <text [attr.x]="PAD.left - 8" [attr.y]="t.y + 4" class="viz-axis" text-anchor="end">{{ compact(t.v) }}</text>
            }
            @for (b of bars(); track b.i) {
              @if (b.hb > 0) {<path [attr.d]="bar(b.x1, b.hb)" class="viz-s1" />}
              @if (b.hc > 0) {<path [attr.d]="bar(b.x2, b.hc)" class="viz-s2" />}
              @if (b.showLabel) {
                <text [attr.x]="b.cx" [attr.y]="H - 8" class="viz-axis" text-anchor="middle">{{ fmtShort()(points()[b.i].label) }}</text>
              }
              <rect [attr.x]="b.gx" [attr.y]="PAD.top" [attr.width]="b.gw" [attr.height]="plotH" fill="transparent"
                    (mouseenter)="hover.set(b.i)" />
            }
            @if (hover() >= 0) {
              <rect [attr.x]="bars()[hover()].gx" [attr.y]="PAD.top" [attr.width]="bars()[hover()].gw" [attr.height]="plotH"
                    class="viz-hover" pointer-events="none" />
            }
            <line [attr.x1]="PAD.left" [attr.x2]="W - PAD.right" [attr.y1]="baseY" [attr.y2]="baseY" class="viz-base" />
          </svg>
          @if (hover() >= 0) {
            <div class="viz-tip" [style.left.%]="tipLeft()">
              <strong>{{ fmt()(points()[hover()].label) }}</strong>
              <div><i class="swatch s1"></i>Billed <b>{{ points()[hover()].billed | currency: 'INR' }}</b></div>
              <div><i class="swatch s2"></i>Collected <b>{{ points()[hover()].collected | currency: 'INR' }}</b></div>
              <div class="muted">{{ points()[hover()].invoiceCount }} invoices</div>
            </div>
          }
        </div>
      }
    </figure>
  `,
})
export class RevenueChart {
  readonly title = input.required<string>();
  readonly points = input.required<RevenuePoint[]>();
  /** Full and short label formatters for the period key (e.g. "2026-10-12", "2026-10", "2026"). */
  readonly fmt = input<(l: string) => string>((l) => l);
  readonly fmtShort = input<(l: string) => string>((l) => l);

  protected readonly W = W;
  protected readonly H = H;
  protected readonly PAD = PAD;
  protected readonly plotH = H - PAD.top - PAD.bottom;
  protected readonly baseY = H - PAD.bottom;
  protected readonly hover = signal(-1);
  protected readonly showTable = signal(false);

  private readonly max = computed(() => niceMax(Math.max(0, ...this.points().map((p) => Math.max(p.billed, p.collected)))));

  protected readonly ticks = computed(() => {
    const max = this.max();
    return [0, 0.25, 0.5, 0.75, 1].map((f) => ({ v: max * f, y: this.baseY - f * this.plotH }));
  });

  protected readonly bars = computed(() => {
    const pts = this.points();
    const n = Math.max(pts.length, 1);
    const plotW = W - PAD.left - PAD.right;
    const gw = plotW / n;
    const barW = Math.max(2, Math.min(18, (gw - 6) / 2 - 1));
    const every = Math.ceil(n / 8);
    const max = this.max();
    return pts.map((p, i) => {
      const gx = PAD.left + i * gw;
      const cx = gx + gw / 2;
      return {
        i,
        gx,
        gw,
        cx,
        x1: cx - barW - 1,
        x2: cx + 1,
        barW,
        hb: max ? (p.billed / max) * this.plotH : 0,
        hc: max ? (p.collected / max) * this.plotH : 0,
        // Every k-th label plus the last one, dropping a tick label that would crowd the last.
        showLabel: i === n - 1 || (i % every === 0 && n - 1 - i >= Math.max(1, every * 0.75)),
      };
    });
  });

  protected readonly tipLeft = computed(() => {
    const b = this.bars()[this.hover()];
    if (!b) return 0;
    return Math.min(78, Math.max(2, (b.cx / W) * 100 - 10));
  });

  /** Bar with a 4px rounded data end, square at the baseline. */
  protected bar(x: number, h: number): string {
    const w = this.bars()[0]?.barW ?? 8;
    const r = Math.min(4, w / 2, h);
    const y = this.baseY - h;
    return `M${x},${this.baseY}V${y + r}Q${x},${y} ${x + r},${y}H${x + w - r}Q${x + w},${y} ${x + w},${y + r}V${this.baseY}Z`;
  }

  protected compact(v: number): string {
    if (v >= 1e7) return `₹${+(v / 1e7).toFixed(1)}Cr`;
    if (v >= 1e5) return `₹${+(v / 1e5).toFixed(1)}L`;
    if (v >= 1e3) return `₹${+(v / 1e3).toFixed(1)}K`;
    return `₹${Math.round(v)}`;
  }
}

function niceMax(v: number): number {
  if (v <= 0) return 1000;
  const pow = 10 ** Math.floor(Math.log10(v));
  const n = v / pow;
  const step = n <= 1 ? 1 : n <= 2 ? 2 : n <= 4 ? 4 : n <= 5 ? 5 : 10;
  return step * pow;
}
