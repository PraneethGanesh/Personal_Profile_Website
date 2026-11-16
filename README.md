# Praneeth Ganesh — Angular portfolio

Angular 21 migration of the original HTML/CSS/JavaScript portfolio. Uses standalone components, strict TypeScript templates, Angular signals, and template-driven form validation. No global Angular CLI is required.

## Run
Requires Node.js 20.19+, 22.12+, or a supported newer LTS version.

```powershell
npm.cmd install
npm.cmd start
```

Open http://localhost:4200. Use npm.cmd on Windows if PowerShell blocks npm.ps1.

## Build
The portfolio can deploy without a backend. Contact submissions remain unavailable until you set your hosted backend's public HTTPS origin:

```powershell
$env:CONTACT_API_URL = 'https://your-real-backend.example.com'
npm.cmd run build
```

For a local development bundle, use `npm.cmd run build:local`. Local `npm.cmd start` does not require this environment variable.

See [the contact form launch guide](docs/deployment.md) for local email verification, SMTP configuration, backend hosting, and Vercel settings. The frontend build must not contain email credentials.

Static output is in dist/portfolio/browser. Deploy its contents to your static host. For hosting under a repository path, use `npm.cmd run build:subpath`. Keep the public asset directory structure and filename casing intact.

## Structure
- src/app/components: header, home, about, experience, projects, skills, certificates, contact, footer.
- src/styles.css: migrated original theme with accessibility and responsive adjustments.
- public/image: original photographs and certificate previews.
- public/Certificates: original certificate PDFs.
- public/favicon_io: original icons and corrected web manifest.

## Behavior
The portfolio now includes these enhancements:
- A light/dark/system theme switcher that remembers the preference locally and follows OS changes in system mode.
- Active-section navigation, a reading-progress bar, and a floating back-to-top link.
- Project search across names, technologies, and features, combined with category filters, result counts, and an empty state.
- Six project cards covering backend services, AI research, GUI development, DevOps practice, and this Angular portfolio. Edit the typed project collection in `src/app/components/projects/projects.component.ts` to add future work.
- A contact form with character count, pending state, clear-form control, and Kafka-backed email delivery through a separate Spring Boot backend.

Mobile navigation, about and project expansions, skill expansions, and the rotating typing text use Angular state. The typing timer is cleaned up on component destruction and respects reduced-motion settings. Boxicons is installed locally rather than loaded from a CDN.

The contact form sends to `POST /api/contact`. See [the local contact flow guide](docs/contact-form.md) for Docker Compose startup, Gmail configuration, verification commands, and delivery guarantees. API and worker use separate Spring profiles and can be stopped independently.

Professional content now reflects the supplied résumé, including work experience, education, research, projects, and training. GitHub links point to the supplied public profile and its listed repositories. The résumé PDF stays outside this project; the old Overleaf résumé link has been removed. Personal address, phone number, and résumé email are excluded from the public profile. Canonical www redirects belong in hosting configuration and are not imposed in the Angular app.

The original HTML website is untouched.

