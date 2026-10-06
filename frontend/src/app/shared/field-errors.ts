import { Component, input } from '@angular/core';

/** The part of a Signal Forms field state this component needs. */
interface FieldStateLike {
  touched(): boolean;
  invalid(): boolean;
  errors(): readonly { kind: string; message?: string }[];
}

@Component({
  selector: 'app-field-errors',
  template: `
    @if (state().touched() && state().invalid()) {
      @for (error of state().errors(); track error.kind) {
        <p class="mt-1 text-sm text-red-700" role="alert">{{ error.message }}</p>
      }
    }
  `,
})
export class FieldErrors {
  readonly state = input.required<FieldStateLike>();
}
