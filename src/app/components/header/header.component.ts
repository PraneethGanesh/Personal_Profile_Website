import { Component, HostListener, afterNextRender, inject, signal } from '@angular/core';
import { ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html'
})
export class HeaderComponent {
  readonly theme = inject(ThemeService);
  readonly menuOpen = signal(false);
  readonly activeSection = signal('home');
  readonly progress = signal(0);
  readonly showBackToTop = signal(false);
  readonly sections = [
    { id: 'home', label: 'Home' },
    { id: 'about', label: 'About' },
    { id: 'experience', label: 'Experience' },
    { id: 'projects', label: 'Projects' },
    { id: 'skills', label: 'Skills' },
    { id: 'certificates', label: 'Certificates' },
    { id: 'contact', label: 'Contact' }
  ];

  constructor() { afterNextRender(() => this.updateScroll()); }

  @HostListener('window:scroll')
  @HostListener('window:resize')
  updateScroll(): void {
    this.menuOpen.set(false);
    const scrollable = document.documentElement.scrollHeight - window.innerHeight;
    this.progress.set(scrollable > 0 ? Math.min(100, Math.max(0, window.scrollY / scrollable * 100)) : 0);
    this.showBackToTop.set(window.scrollY > 600);
    let current = 'home';
    const headerHeight = document.querySelector('header')?.getBoundingClientRect().height ?? 80;
    for (const section of this.sections) {
      if ((document.getElementById(section.id)?.getBoundingClientRect().top ?? Infinity) <= headerHeight + 40) {
        current = section.id;
      }
    }
    if (scrollable > 0 && window.scrollY >= scrollable - 2) { current = 'contact'; }
    this.activeSection.set(current);
  }

  @HostListener('document:keydown.escape')
  closeMenu(): void { this.menuOpen.set(false); }
}

