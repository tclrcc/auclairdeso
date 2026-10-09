import { Component, computed, input } from '@angular/core';

const RADIUS = 20;

/**
 * A waxing moon, lit from the right: 0 is a new moon, 0.5 a first quarter, 1 a full moon.
 * The lit part is the right half-disc, closed by the terminator: a half-ellipse whose width
 * shrinks to zero at the quarter, then bulges to the other side.
 */
@Component({
  selector: 'app-moon-phase',
  template: `
    <svg viewBox="-24 -24 48 48" aria-hidden="true" class="size-full overflow-visible">
      <circle [attr.r]="radius" fill="none" stroke="currentColor" stroke-opacity="0.45" stroke-width="1" />
      <path [attr.d]="litPart()" fill="currentColor" />
    </svg>
  `,
})
export class MoonPhase {
  readonly illuminated = input.required<number>();

  protected readonly radius = RADIUS;

  protected readonly litPart = computed(() => {
    const lit = Math.min(1, Math.max(0, this.illuminated()));
    const terminator = RADIUS * Math.abs(1 - 2 * lit);
    const sweep = lit > 0.5 ? 1 : 0;
    return (
      `M 0 ${-RADIUS} A ${RADIUS} ${RADIUS} 0 0 1 0 ${RADIUS} ` +
      `A ${terminator} ${RADIUS} 0 0 ${sweep} 0 ${-RADIUS} Z`
    );
  });
}
