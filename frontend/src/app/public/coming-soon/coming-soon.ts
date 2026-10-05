import { Component, inject } from '@angular/core';
import { Meta } from '@angular/platform-browser';

@Component({
  selector: 'app-coming-soon',
  templateUrl: './coming-soon.html',
})
export class ComingSoon {
  private readonly meta = inject(Meta);

  constructor() {
    this.meta.updateTag({
      name: 'description',
      content: 'Au clair de So - le site arrive bientôt. Suivez actualité sur Instagram et Facebook',
    });
  }
}
