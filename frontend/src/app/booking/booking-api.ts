import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AppointmentView, BookingRequest } from './booking';

@Service()
export class BookingApi {
  private readonly http = inject(HttpClient);

  /** Sends the request as JSON and, when needed, the portrait, in one multipart body. */
  book(request: BookingRequest, photo: File | null): Promise<AppointmentView> {
    const body = new FormData();
    body.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    if (photo) {
      body.append('photo', photo);
    }
    return firstValueFrom(this.http.post<AppointmentView>('/api/bookings', body));
  }

  cancel(id: number): Promise<AppointmentView> {
    return firstValueFrom(this.http.post<AppointmentView>(`/api/bookings/${id}/cancel`, null));
  }
}
