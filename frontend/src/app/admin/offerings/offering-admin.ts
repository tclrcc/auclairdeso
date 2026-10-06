import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AdminOffering, OfferingDraft } from './admin-offering';

@Service()
export class OfferingAdmin {
  private readonly http = inject(HttpClient);

  create(draft: OfferingDraft): Promise<AdminOffering> {
    return firstValueFrom(this.http.post<AdminOffering>('/api/admin/offerings', draft));
  }

  update(slug: string, draft: OfferingDraft): Promise<AdminOffering> {
    return firstValueFrom(
      this.http.put<AdminOffering>(`/api/admin/offerings/${encodeURIComponent(slug)}`, draft),
    );
  }
}
