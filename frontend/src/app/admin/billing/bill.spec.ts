import { exportUrl, formatExactEuros, formatLongDate, monthlyTotals, ReceiptEntry } from './bill';

// Intl puts non-breaking spaces in amounts: normalise them for readable assertions.
const normalise = (value: string) => value.replace(/\s/g, ' ');

const entry = (paidOn: string, amountCents: number): ReceiptEntry => ({
  billId: 1,
  appointmentId: 1,
  paidOn,
  number: '2026-0001',
  clientName: 'Alice Martin',
  description: 'Consultation',
  method: 'TRANSFER',
  amountCents,
});

describe('monthlyTotals', () => {
  it('adds up each month that received something, in calendar order', () => {
    const totals = monthlyTotals([entry('2026-10-02', 8000), entry('2026-03-15', 4000), entry('2026-10-20', 5050)]);

    expect(totals).toEqual([
      { month: 3, label: 'mars', totalCents: 4000 },
      { month: 10, label: 'octobre', totalCents: 13050 },
    ]);
  });
});

describe('bill formats', () => {
  it('always shows the cents of an amount', () => {
    expect(normalise(formatExactEuros(8000))).toBe('80,00 €');
    expect(normalise(formatExactEuros(123456))).toBe('1 234,56 €');
  });

  it('writes a date in full', () => {
    expect(formatLongDate('2026-10-18')).toBe('18 octobre 2026');
  });

  it('points to the CSV of a year', () => {
    expect(exportUrl(2026)).toBe('/api/admin/receipts/export?year=2026');
  });
});
