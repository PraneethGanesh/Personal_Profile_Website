import { Component, OnInit, OnDestroy, signal } from '@angular/core';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit, OnDestroy {
  readonly role = signal('Software Engineer');
  private timer: ReturnType<typeof setTimeout> | undefined;
  private readonly roles = ['Software Engineer', 'Frontend Engineer', 'Backend Engineer', 'DevOps Engineer', 'AI Engineer'];
  private roleIndex = 0;
  private deleting = true;

  ngOnInit(): void {
    if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      this.timer = setTimeout(() => this.tick(), 1000);
    }
  }

  ngOnDestroy(): void { clearTimeout(this.timer); }

  private tick(): void {
    const target = this.roles[this.roleIndex]!;
    const current = this.role();
    let delay = 60;
    if (this.deleting) {
      this.role.set(current.slice(0, -1));
      if (!this.role()) {
        this.deleting = false;
        this.roleIndex = (this.roleIndex + 1) % this.roles.length;
      }
    } else {
      this.role.set(target.slice(0, current.length + 1));
      if (this.role() === target) { this.deleting = true; delay = 1000; }
    }
    this.timer = setTimeout(() => this.tick(), delay);
  }
}

