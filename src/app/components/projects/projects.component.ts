import { Component, computed, signal } from '@angular/core';

interface Project {
  id: string;
  title: string;
  icon: string;
  category: string;
  description: string;
  tags: string[];
  details: string[];
  url?: string;
  linkLabel?: string;
}

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html'
})
export class ProjectsComponent {
  readonly query = signal('');
  readonly category = signal('All');
  readonly categories = ['All', 'Backend', 'Frontend', 'AI & Research', 'DevOps'];
  readonly expanded = signal<ReadonlySet<string>>(new Set());
  readonly projects: Project[] = [
    {
        "id": "drms",
        "title": "Device Repair and Management System",
        "icon": "bx-devices",
        "category": "Backend",
        "description": "A microservices application for managing users, vendors, devices, orders, repairs, notifications, and audits.",
        "tags": [
            "Java",
            "Spring Boot",
            "Kafka",
            "MySQL",
            "Microservices"
        ],
        "details": [
            "Built with Eureka service discovery, Spring Cloud Gateway, REST APIs, and JWT authentication.",
            "Used Apache Kafka for event-driven inter-service communication.",
            "Integrated Micrometer, Prometheus, and Grafana for monitoring and observability.",
            "Project period: May – June 2026."
        ],
        "url": "https://github.com/PraneethGanesh/Device_Repair_and_Management_System",
        "linkLabel": "View on GitHub"
    },
    {
        "id": "employees",
        "title": "Employee Management System",
        "icon": "bx-data",
        "category": "Backend",
        "description": "A Spring Boot system for employee records, leave requests, deliveries, vendors, and subscriptions.",
        "tags": [
            "Spring Security",
            "JWT",
            "OAuth 2.0",
            "MySQL",
            "2FA"
        ],
        "details": [
            "Implemented JWT, OAuth 2.0, API key, session, and TOTP-based two-factor authentication.",
            "Used Spring Data JPA and DTOs with centralized exception handling.",
            "Added scheduled tasks and Swagger/OpenAPI documentation.",
            "Project period: March – May 2026."
        ],
        "url": "https://github.com/PraneethGanesh/SmartEmployeeManagementSystem",
        "linkLabel": "View on GitHub"
    },
    {
        "id": "blueprint",
        "title": "AI Blueprint to 3D Models",
        "icon": "bx-cube-alt",
        "category": "AI & Research",
        "description": "Led a team exploring automated conversion of 2D blueprints into 3D models for civil applications using deep learning.",
        "tags": [
            "AI",
            "Deep Learning",
            "3D Modeling",
            "IEEE AIDE 2026"
        ],
        "details": [
            "Coordinated project planning, implementation, technical documentation, and research activities during 2025–2026.",
            "Presented the work at the IEEE International Conference on Artificial Intelligence and Data Engineering in February 2026.",
            "Co-authored the research paper published in IEEE Xplore. DOI: 10.1109/AIDE69088.2026.11544904."
        ],
        "url": "https://doi.org/10.1109/AIDE69088.2026.11544904",
        "linkLabel": "Read Research Paper"
    },
    {
        "id": "circuit",
        "title": "Electronic Component Visualizer",
        "icon": "bx-chip",
        "category": "Frontend",
        "description": "A Python and Tkinter GUI for exploring electronic components, device connectivity, and architecture.",
        "tags": [
            "Python",
            "Tkinter",
            "GUI",
            "Visualization"
        ],
        "details": [
            "Developed during the GUI internship at RDL Technologies.",
            "Built interactive visualization tools to support system understanding and troubleshooting.",
            "Collaborated on connectivity workflows and usability improvements."
        ],
        "url": "https://github.com/PraneethGanesh/Project_GUI_circuit_designer",
        "linkLabel": "View on GitHub"
    },
    {
        "id": "jenkins",
        "title": "Jenkins CI/CD Demo",
        "icon": "bx-git-branch",
        "category": "DevOps",
        "description": "A Java demo project for practising Jenkins and the continuous integration workflow.",
        "tags": [
            "Java",
            "Jenkins",
            "CI/CD"
        ],
        "details": [
            "A dedicated repository for Jenkins experimentation and learning.",
            "Part of my ongoing practice with delivery automation and DevOps tooling."
        ],
        "url": "https://github.com/PraneethGanesh/jenkinsdemo",
        "linkLabel": "View on GitHub"
    },
    {
        "id": "portfolio",
        "title": "Personal Portfolio",
        "icon": "bx-code-alt",
        "category": "Frontend",
        "description": "A responsive Angular portfolio with a separate Spring Boot and Kafka contact notification backend.",
        "tags": [
            "Angular",
            "TypeScript",
            "Spring Boot",
            "Kafka",
            "Docker"
        ],
        "details": [
            "Standalone components and signals power interactive content and project filtering.",
            "Includes saved theme preferences, keyboard navigation, and reduced-motion support.",
            "The contact API queues submissions through Kafka for a separate email worker.",
            "Docker Compose provides a local environment for the backend and broker."
        ]
    }
];
  readonly filteredProjects = computed(() => {
    const query = this.query().trim().toLowerCase();
    return this.projects.filter((project) =>
      (this.category() === 'All' || project.category === this.category()) &&
      [project.title, project.description, project.category, ...project.tags, ...project.details].join(' ').toLowerCase().includes(query)
    );
  });

  resetFilters(): void { this.query.set(''); this.category.set('All'); }

  toggle(id: string): void {
    this.expanded.update((current) => {
      const next = new Set(current);
      if (next.has(id)) { next.delete(id); } else { next.add(id); }
      return next;
    });
  }
}

