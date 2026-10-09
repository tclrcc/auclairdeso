import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AdminAppointment } from './admin-appointment';

export type AgendaAction = 'confirm' | 'decline' | 'cancel' | 'complete' | 'no-show';

@Service()
export class AgendaApi {
  private readonly http = inject(HttpClient);

  apply(id: number, action: AgendaAction): Promise<AdminAppointment> {
    return firstValueFrom(this.http.post<AdminAppointment>(`/api/admin/appointments/${id}/${action}`, null));
  }
}
