import { Component, signal } from '@angular/core';

@Component({
  selector: 'app-skills',
  templateUrl: './skills.component.html'
})
export class SkillsComponent {
  readonly skills = [
    {
        "title": "Frontend Development",
        "icon": "bxl-angular",
        "summary": "Responsive interfaces with Angular, React, and TypeScript.",
        "details": "HTML, CSS, JavaScript, Bootstrap, component-based UI development, accessible controls, and responsive layouts."
    },
    {
        "title": "Backend & API Development",
        "icon": "bxl-java",
        "summary": "Java services, secure APIs, and Spring Boot applications.",
        "details": "Core and Advanced Java, Spring Boot, Spring Security, Hibernate, JDBC, REST APIs, JWT, OAuth 2.0, and TOTP-based 2FA."
    },
    {
        "title": "Data & Messaging",
        "icon": "bx-data",
        "summary": "Persistent data and event-driven application workflows.",
        "details": "MySQL, Spring Data JPA, database schema design, JSON data models, Apache Kafka, and RabbitMQ."
    },
    {
        "title": "DevOps & Cloud",
        "icon": "bx-cloud",
        "summary": "Container workflows, delivery automation, and cloud-ready application design.",
        "details": "Docker, Jenkins CI/CD practice, Git, Linux command-line tools, and Kubernetes deployment practice. Continuing to develop cloud and deployment skills."
    },
    {
        "title": "AI & Research",
        "icon": "bx-brain",
        "summary": "Deep learning research for blueprint-to-3D model conversion.",
        "details": "Project leadership, research coordination, technical documentation, and presenting AI work at IEEE AIDE 2026."
    },
    {
        "title": "Observability & Tools",
        "icon": "bx-line-chart",
        "summary": "Monitoring services and creating reusable reporting workflows.",
        "details": "Micrometer, Prometheus, Grafana, Swagger/OpenAPI, Jaspersoft Studio, and Python/Tkinter visualization tools."
    }
];
  readonly expanded = signal<ReadonlySet<number>>(new Set());

  toggle(index: number): void {
    this.expanded.update((current) => {
      const next = new Set(current);
      if (next.has(index)) { next.delete(index); } else { next.add(index); }
      return next;
    });
  }
}

