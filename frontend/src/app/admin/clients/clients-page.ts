import { Component, computed, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { ClientSummary } from '../agenda/admin-appointment';
import { ClientFilter, clientsUrl, FILTER_LABELS, LIST_LIMIT } from './admin-client';

const EMPTY_LIST: Record<ClientFilter, string> = {
  ALL: 'Aucune cliente pour l’instant. Elles apparaissent ici dès leur première demande, '
    + 'ou dès que vous saisissez un rendez-vous pour elles.',
  TRUSTED: 'Aucune cliente de confiance pour l’instant.',
  BLOCKED: 'Aucune cliente bloquée.',
};

@Component({
  selector: 'app-clients-page',
  imports: [RouterLink],
  templateUrl: './clients-page.html',
})
export class ClientsPage {
  protected readonly filters: readonly ClientFilter[] = ['ALL', 'TRUSTED', 'BLOCKED'];
  protected readonly filterLabels = FILTER_LABELS;

  protected readonly query = signal('');
  protected readonly filter = signal<ClientFilter>('ALL');

  /** A new request at each keystroke: the previous one is cancelled. */
  protected readonly clients = httpResource<ClientSummary[]>(() => clientsUrl(this.query(), this.filter()), {
    defaultValue: [],
  });
  protected readonly tooShort = computed(() => this.query().trim().length === 1);
  protected readonly truncated = computed(() => this.clients.value().length >= LIST_LIMIT);
  protected readonly emptyMessage = computed(() => {
    const text = this.query().trim();
    return text ? `Aucune cliente ne correspond à « ${text} ».` : EMPTY_LIST[this.filter()];
  });

  protected onSearch(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }
}
