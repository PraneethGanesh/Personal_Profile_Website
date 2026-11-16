import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, computed, effect, inject, signal } from '@angular/core';

type ThemePreference = 'system' | 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly systemTheme = window.matchMedia('(prefers-color-scheme: dark)');
  private readonly systemDark = signal(this.systemTheme.matches);
  readonly preference = signal<ThemePreference>(this.readPreference());
  readonly dark = computed(() => this.preference() === 'dark' || (this.preference() === 'system' && this.systemDark()));

  constructor() {
    const updateSystem = (event: MediaQueryListEvent): void => this.systemDark.set(event.matches);
    this.systemTheme.addEventListener('change', updateSystem);
    inject(DestroyRef).onDestroy(() => this.systemTheme.removeEventListener('change', updateSystem));
    effect(() => {
      this.document.documentElement.dataset['theme'] = this.dark() ? 'dark' : 'light';
      try { localStorage.setItem('portfolio-theme', this.preference()); } catch {}
    });
  }

  cycle(): void {
    const next: Record<ThemePreference, ThemePreference> = { system: 'light', light: 'dark', dark: 'system' };
    this.preference.set(next[this.preference()]);
  }

  private readPreference(): ThemePreference {
    try {
      const stored = localStorage.getItem('portfolio-theme');
      if (stored === 'light' || stored === 'dark') { return stored; }
    } catch {}
    return 'system';
  }
}
