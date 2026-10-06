import { Component, afterNextRender, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthSession } from './core/auth/auth-session';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
})
export class App {
  constructor() {
    const auth = inject(AuthSession);
    afterNextRender(() => {
      void auth.refresh();
    });
  }
}
