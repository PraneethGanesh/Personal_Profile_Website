import { Component } from '@angular/core';

@Component({
  selector: 'app-experience',
  templateUrl: './experience.component.html'
})
export class ExperienceComponent {
  readonly experiences = [
    {
      role: 'Associate Backend Engineer',
      company: 'ScleraVDMS Private Limited',
      period: 'February 2026 – October 2026',
      details: [
        'Created reusable Jaspersoft report templates for runtime PDF and Excel exports.',
        'Used RabbitMQ to queue inspection jobs and improve recovery after scheduler crashes.',
        'Designed device-side offline reminders for expiring checklists.',
        'Designed database schemas and JSON models for versioned procedure templates, inspection configurations, and historical responses.',
        'Contributed to the design of domain-focused microservices for Sclera 2.0.'
      ]
    },
    {
      role: 'Java Programming Trainee',
      company: 'Incture Technologies Pvt Ltd · Industry and Academia Outreach Initiative',
      period: 'July 2025 – August 2025',
      details: [
        'Completed four weeks of intensive Core and Advanced Java training, including OOP, exception handling, collections, and JDBC.',
        'Gained hands-on experience with Hibernate, Spring Boot, and Spring Security.'
      ]
    },
    {
      role: 'GUI Development Intern',
      company: 'RDL Technologies Pvt. Ltd.',
      period: 'August 2024 – November 2024',
      details: [
        'Developed a Tkinter interface for electronic component visualization and interactive workflows.',
        'Built tools to visualize device connectivity and architecture, supporting troubleshooting and system understanding.',
        'Collaborated with the team to improve connectivity workflows and application usability.'
      ]
    }
  ];
}
