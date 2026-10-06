import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CurrentUser } from './current-user';

/**
 * Who is logged in, and the magic link login flow.
 */
@Service()
export class AuthSession {
  private readonly http = inject(HttpClient);

  /** undefined: not checked yet; null: anonymous visitor. */
  private readonly user = signal<CurrentUser | null | undefined>(undefined);

  readonly currentUser = this.user.asReadonly();
  readonly isAuthenticated = computed(() => this.user() != null);

  async refresh(): Promise<CurrentUser | null> {
    try {
      const user = await firstValueFrom(this.http.get<CurrentUser>('/api/me'));
      this.user.set(user);
      return user;
    } catch (error) {
      if (isUnauthorized(error)) {
        this.user.set(null);
        return null;
      }
      throw error;
    }
  }

  async requestMagicLink(email: string): Promise<void> {
    const body = new HttpParams().set('username', email);
    await firstValueFrom(this.http.post<void>('/api/auth/magic-link', body));
  }

  /** Returns false when the token is expired or was already used. */
  async verify(token: string): Promise<boolean> {
    const body = new HttpParams().set('token', token);
    try {
      await firstValueFrom(this.http.post<void>('/api/auth/magic-link/verify', body));
    } catch (error) {
      if (isUnauthorized(error)) {
        return false;
      }
      throw error;
    }
    await this.refresh();
    return true;
  }

  async logout(): Promise<void> {
    await firstValueFrom(this.http.post<void>('/api/auth/logout', null));
    this.user.set(null);
  }
}

function isUnauthorized(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 401;
}
