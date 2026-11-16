import { Component } from '@angular/core';
import { HeaderComponent } from './components/header/header.component';
import { HomeComponent } from './components/home/home.component';
import { AboutComponent } from './components/about/about.component';
import { ExperienceComponent } from './components/experience/experience.component';
import { ProjectsComponent } from './components/projects/projects.component';
import { SkillsComponent } from './components/skills/skills.component';
import { CertificatesComponent } from './components/certificates/certificates.component';
import { ContactComponent } from './components/contact/contact.component';
import { FooterComponent } from './components/footer/footer.component';

@Component({
  selector: 'app-root',
  imports: [HeaderComponent, HomeComponent, AboutComponent, ExperienceComponent, ProjectsComponent, SkillsComponent, CertificatesComponent, ContactComponent, FooterComponent],
  template: `
    <a class="skip-link" href="#home">Skip to content</a>
    <app-header />
    <main><app-home /><app-about /><app-experience /><app-projects /><app-skills /><app-certificates /><app-contact /></main>
    <app-footer />
  `
})
export class AppComponent {}

