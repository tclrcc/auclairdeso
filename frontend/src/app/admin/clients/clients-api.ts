import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ClientCorrection, ClientFile, ClientNote } from './admin-client';

export type ClientAction = 'trust' | 'distrust' | 'block' | 'unblock';

/** The changes made from a client file. Each one answers with the updated file, except the notes. */
@Service()
export class ClientsApi {
  private readonly http = inject(HttpClient);

  apply(id: number, action: ClientAction): Promise<ClientFile> {
    return firstValueFrom(this.http.post<ClientFile>(`/api/admin/clients/${id}/${action}`, null));
  }

  correct(id: number, correction: ClientCorrection): Promise<ClientFile> {
    return firstValueFrom(this.http.put<ClientFile>(`/api/admin/clients/${id}`, correction));
  }

  addNote(id: number, text: string): Promise<ClientNote> {
    return firstValueFrom(this.http.post<ClientNote>(`/api/admin/clients/${id}/notes`, { text }));
  }

  deleteNote(id: number, noteId: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/admin/clients/${id}/notes/${noteId}`));
  }
}
