import { Component } from '@angular/core';
import { CRESCENT_PATH } from './crescent';

interface Ray {
  readonly x1: number;
  readonly y1: number;
  readonly x2: number;
  readonly y2: number;
  readonly long: boolean;
  /** Rank from the top of the eye, to light the rays one after the other. */
  readonly order: number;
}

/** Rays around the eye: they leave an invisible ellipse and point away from the centre. */
function rays(): Ray[] {
  const result: Ray[] = [];
  for (let degrees = 0; degrees < 360; degrees += 10) {
    const angle = (degrees * Math.PI) / 180;
    const [cos, sin] = [Math.cos(angle), Math.sin(angle)];
    if (Math.abs(sin) < 0.3) {
      continue; // the corners of the eye stay clear
    }
    const start = 1 / Math.hypot(cos / 170, sin / 82);
    const long = degrees % 20 === 0;
    const end = start + (long ? 46 : 26);
    result.push({
      x1: round(start * cos),
      y1: round(start * sin),
      x2: round(end * cos),
      y2: round(end * sin),
      long,
      order: Math.round(angleFromTop(degrees) / 10),
    });
  }
  return result;
}

/** In SVG, y points down: the top of the eye is at 270°. Returns a distance between 0 and 180°. */
function angleFromTop(degrees: number): number {
  return Math.abs(((degrees - 270 + 540) % 360) - 180);
}

function round(value: number): number {
  return Math.round(value * 10) / 10;
}

/** The eye of the logo, which opens once when the page appears. Purely decorative. */
@Component({
  selector: 'app-opening-eye',
  templateUrl: './opening-eye.html',
  styleUrl: './opening-eye.css',
})
export class OpeningEye {
  protected readonly rays = rays();
  protected readonly crescent = CRESCENT_PATH;
}
