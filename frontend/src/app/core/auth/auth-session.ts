import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CurrentUser, isStaff } from './current-user';

/**
 * Who is logged in, and the login flows: magic link, then password for staff members.
 */
@Service()
export class AuthSession {
  private readonly http = inject(HttpClient);

  /** undefined: not checked yet; null: anonymous visitor. */
  private readonly user = signal<CurrentUser | null | undefined>(undefined);

  readonly currentUser = this.user.asReadonly();
  readonly isAuthenticated = computed(() => this.user() != null);
  readonly isStaff = computed(() => {
    const user = this.user();
    return user != null && isStaff(user);
  });

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

  /** Second factor for staff members. Returns false when the password is wrong. */
  async confirmPassword(password: string): Promise<boolean> {
    const email = this.user()?.email;
    if (!email) {
      return false;
    }
    const body = new HttpParams().set('username', email).set('password', password);
    try {
      await firstValueFrom(this.http.post<void>('/api/auth/password', body));
    } catch (error) {
      if (isUnauthorized(error)) {
        return false;
      }
      throw error;
    }
    await this.refresh();
    return true;
  }

  /** First password of a staff member, then immediate use of it as the second factor. */
  async definePassword(password: string): Promise<void> {
    await firstValueFrom(this.http.put<void>('/api/account/password', { password }));
    await this.confirmPassword(password);
  }

  async logout(): Promise<void> {
    await firstValueFrom(this.http.post<void>('/api/auth/logout', null));
    this.user.set(null);
  }
}

function isUnauthorized(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 401;
}
