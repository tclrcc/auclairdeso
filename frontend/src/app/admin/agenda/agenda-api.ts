import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AdminAppointment } from './admin-appointment';

@Service()
export class AgendaApi {
  private readonly http = inject(HttpClient);

  confirm(id: number): Promise<AdminAppointment> {
    return firstValueFrom(this.http.post<AdminAppointment>(`/api/admin/appointments/${id}/confirm`, null));
  }

  decline(id: number): Promise<AdminAppointment> {
    return firstValueFrom(this.http.post<AdminAppointment>(`/api/admin/appointments/${id}/decline`, null));
  }
}
