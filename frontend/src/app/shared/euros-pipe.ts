import { Pipe, PipeTransform } from '@angular/core';

const wholeEuros = new Intl.NumberFormat('fr-FR', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
});

const euros = new Intl.NumberFormat('fr-FR', {
  style: 'currency',
  currency: 'EUR',
});

/** Formats an amount in euro cents: 8000 → "80 €", 4550 → "45,50 €". */
@Pipe({ name: 'euros' })
export class EurosPipe implements PipeTransform {
  transform(cents: number | null | undefined): string {
    if (cents == null) {
      return '';
    }
    return (cents % 100 === 0 ? wholeEuros : euros).format(cents / 100);
  }
}
