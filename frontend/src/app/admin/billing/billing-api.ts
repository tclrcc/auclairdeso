import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Bill, PaymentMethod } from './bill';

/** Payments of the bills. Each call answers with the updated bill. */
@Service()
export class BillingApi {
  private readonly http = inject(HttpClient);

  pay(billId: number, method: PaymentMethod, paidOn: string): Promise<Bill> {
    return firstValueFrom(this.http.post<Bill>(`/api/admin/bills/${billId}/payment`, { method, paidOn }));
  }

  cancelPayment(billId: number): Promise<Bill> {
    return firstValueFrom(this.http.delete<Bill>(`/api/admin/bills/${billId}/payment`));
  }
}
